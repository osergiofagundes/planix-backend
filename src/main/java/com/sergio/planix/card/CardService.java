package com.sergio.planix.card;

import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.card.dto.CardUpdateRequest;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.history.CardChange;
import com.sergio.planix.history.CardChangeRepository;
import com.sergio.planix.history.dto.CardChangeResponse;
import com.sergio.planix.list.BoardList;
import com.sergio.planix.list.BoardListRepository;
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

    public CardService(CardRepository cardRepo, BoardListRepository listRepo,
                       CardChangeRepository changeRepo) {
        this.cardRepo = cardRepo;
        this.listRepo = listRepo;
        this.changeRepo = changeRepo;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listByList(Long listId) {
        if (!listRepo.existsById(listId)) {
            throw new NotFoundException("Lista %d não encontrada".formatted(listId));
        }
        return cardRepo.findByListIdOrderByPositionAsc(listId).stream().map(CardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CardResponse get(Long id) {
        return CardResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CardChangeResponse> listChanges(Long cardId) {
        if (!cardRepo.existsById(cardId)) {
            throw new NotFoundException("Cartão %d não encontrado".formatted(cardId));
        }
        return changeRepo.findByCardIdOrderByChangedAtDesc(cardId).stream()
                .map(CardChangeResponse::from).toList();
    }

    public CardResponse create(Long listId, CardCreateRequest req) {
        BoardList list = listRepo.findById(listId)
                .orElseThrow(() -> new NotFoundException("Lista %d não encontrada".formatted(listId)));
        int position = cardRepo.countByListId(listId);
        return CardResponse.from(cardRepo.save(new Card(list, req.title(), position)));
    }

    public CardResponse update(Long cardId, CardUpdateRequest req) {
        Card card = findOrThrow(cardId);
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
        Card card = findOrThrow(cardId);
        if (card.isCompleted() == completed) return CardResponse.from(card);

        changeRepo.save(new CardChange(card, "completed",
                String.valueOf(card.isCompleted()), String.valueOf(completed)));

        card.setCompleted(completed);
        card.setCompletedAt(completed ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        return CardResponse.from(card);
    }

    public void move(Long cardId, Long targetListId, int newPosition) {
        Card card = findOrThrow(cardId);
        Long sourceListId = card.getList().getId();

        if (!sourceListId.equals(targetListId)) {
            BoardList target = listRepo.findById(targetListId)
                    .orElseThrow(() -> new NotFoundException("Lista %d não encontrada".formatted(targetListId)));
            changeRepo.save(new CardChange(card, "list_id",
                    String.valueOf(sourceListId), String.valueOf(targetListId)));
            card.setList(target);
            cardRepo.flush();
            reindex(sourceListId, cardId);
        }
        insertAt(card, targetListId, newPosition);
    }

    public void delete(Long cardId) {
        Card card = findOrThrow(cardId);
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
            acc.add(new CardChange(card, field, oldValue, newValue));
        }
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private Card findOrThrow(Long id) {
        return cardRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(id)));
    }
}
