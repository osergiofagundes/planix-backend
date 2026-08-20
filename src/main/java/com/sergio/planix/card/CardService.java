package com.sergio.planix.card;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.card.dto.CardUpdateRequest;
import com.sergio.planix.common.exception.CrossBoardMoveException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.card.dto.CardChangeResponse;
import com.sergio.planix.list.BoardList;
import com.sergio.planix.list.BoardListRepository;
import com.sergio.planix.notification.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CardService {

    private final CardRepository cardRepo;
    private final BoardListRepository listRepo;
    private final CardChangeRepository changeRepo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;
    private final CurrentUser currentUser;
    private final NotificationPublisher notifications;

    public CardService(CardRepository cardRepo, BoardListRepository listRepo,
                       CardChangeRepository changeRepo, CardAccess cardAccess,
                       BoardAccess boardAccess, CurrentUser currentUser,
                       NotificationPublisher notifications) {
        this.cardRepo = cardRepo;
        this.listRepo = listRepo;
        this.changeRepo = changeRepo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
        this.currentUser = currentUser;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listByList(Long listId) {
        findListOrThrow(listId);
        return cardRepo.findByListIdOrderByPositionAsc(listId).stream().map(CardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CardResponse get(Long id) {
        return CardResponse.from(cardAccess.require(id));
    }

    @Transactional(readOnly = true)
    public List<CardChangeResponse> listChanges(Long cardId) {
        cardAccess.require(cardId);
        return changeRepo.findByCardIdOrderByChangedAtDesc(cardId).stream()
                .map(CardChangeResponse::from).toList();
    }

    public CardResponse create(Long listId, CardCreateRequest req) {
        BoardList list = findListOrThrow(listId);
        int position = cardRepo.countByListId(listId);
        return CardResponse.from(cardRepo.save(new Card(list, req.title(), position)));
    }

    public CardResponse update(Long cardId, CardUpdateRequest req) {
        Card card = cardAccess.require(cardId);
        Priority newPriority = req.priority() == null ? Priority.NONE : req.priority();
        List<CardChange> changes = new ArrayList<>();

        recordIfChanged(changes, card, "title", card.getTitle(), req.title());
        recordIfChanged(changes, card, "description", card.getDescription(), req.description());
        recordIfChanged(changes, card, "priority", card.getPriority().name(), newPriority.name());
        recordIfChanged(changes, card, "due_date", str(card.getDueDate()), str(req.dueDate()));

        card.setTitle(req.title());
        card.setDescription(req.description());
        card.setPriority(newPriority);
        card.setDueDate(req.dueDate());

        changeRepo.saveAll(changes);
        return CardResponse.from(card);
    }

    public CardResponse setCompleted(Long cardId, boolean completed) {
        Card card = cardAccess.require(cardId);
        if (card.isCompleted() == completed) return CardResponse.from(card);

        changeRepo.save(new CardChange(card, currentUser.reference(), "completed",
                String.valueOf(card.isCompleted()), String.valueOf(completed)));

        card.setCompleted(completed);
        card.setCompletedAt(completed ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        return CardResponse.from(card);
    }

    public void move(Long cardId, Long targetListId, int newPosition) {
        Card card = cardAccess.require(cardId);
        Long sourceListId = card.getList().getId();

        if (!sourceListId.equals(targetListId)) {
            BoardList target = findListOrThrow(targetListId);

            if (!target.getBoard().getId().equals(card.getList().getBoard().getId())) {
                throw new CrossBoardMoveException(
                        "A lista de destino precisa pertencer ao mesmo quadro do cartão");
            }
            String fromList = card.getList().getName();
            changeRepo.save(new CardChange(card, currentUser.reference(), "list_id",
                    String.valueOf(sourceListId), String.valueOf(targetListId)));
            card.setList(target);
            cardRepo.flush();
            reindex(sourceListId, cardId);
            notifications.cardMoved(card, fromList, target.getName());
        }
        insertAt(card, targetListId, newPosition);
    }

    public void delete(Long cardId) {
        Card card = cardAccess.require(cardId);
        Long listId = card.getList().getId();

        cardRepo.delete(card);
        cardRepo.flush();
        reindex(listId, cardId);
    }

    private void reindex(Long listId, Long ignoreCardId) {
        List<Card> siblings = siblingsOf(listId, ignoreCardId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    private void insertAt(Card card, Long listId, int newPosition) {
        List<Card> siblings = siblingsOf(listId, card.getId());
        int target = Math.max(0, Math.min(newPosition, siblings.size()));
        siblings.add(target, card);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    private List<Card> siblingsOf(Long listId, Long ignoreCardId) {
        List<Card> siblings = new ArrayList<>(cardRepo.findByListIdOrderByPositionAsc(listId));
        if (ignoreCardId != null) {
            siblings.removeIf(c -> ignoreCardId.equals(c.getId()));
        }
        return siblings;
    }

    private void recordIfChanged(List<CardChange> acc, Card card, String field,
                                 String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            acc.add(new CardChange(card, currentUser.reference(), field, oldValue, newValue));
        }
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private BoardList findListOrThrow(Long listId) {
        BoardList list = listRepo.findById(listId)
                .orElseThrow(() -> naoEncontrada(listId));
        if (!boardAccess.isMember(list.getBoard().getId())) {
            throw naoEncontrada(listId);
        }
        return list;
    }

    private NotFoundException naoEncontrada(Long listId) {
        return new NotFoundException("Lista %d não encontrada".formatted(listId));
    }
}
