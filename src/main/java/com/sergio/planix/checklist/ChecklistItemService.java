package com.sergio.planix.checklist;

import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.checklist.dto.ChecklistItemRequest;
import com.sergio.planix.checklist.dto.ChecklistItemResponse;
import com.sergio.planix.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChecklistItemService {

    private final ChecklistItemRepository repo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;

    public ChecklistItemService(ChecklistItemRepository repo, CardAccess cardAccess,
                                BoardAccess boardAccess) {
        this.repo = repo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> listByCard(Long cardId) {
        cardAccess.require(cardId);
        return repo.findByCardIdOrderByPositionAsc(cardId).stream()
                .map(ChecklistItemResponse::from).toList();
    }

    public ChecklistItemResponse create(Long cardId, ChecklistItemRequest req) {
        Card card = cardAccess.require(cardId);
        int position = repo.countByCardId(cardId);
        return ChecklistItemResponse.from(repo.save(new ChecklistItem(card, req.text(), position)));
    }

    public ChecklistItemResponse update(Long id, ChecklistItemRequest req) {
        ChecklistItem item = findOrThrow(id);
        item.setText(req.text());
        return ChecklistItemResponse.from(item);
    }

    public ChecklistItemResponse toggle(Long id) {
        ChecklistItem item = findOrThrow(id);
        item.setDone(!item.isDone());
        return ChecklistItemResponse.from(item);
    }

    public void move(Long id, int newPosition) {
        ChecklistItem item = findOrThrow(id);
        Long cardId = item.getCard().getId();

        List<ChecklistItem> siblings = repo.findByCardIdOrderByPositionAsc(cardId);
        siblings.remove(item);
        int target = Math.max(0, Math.min(newPosition, siblings.size()));
        siblings.add(target, item);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    public void delete(Long id) {
        ChecklistItem item = findOrThrow(id);
        Long cardId = item.getCard().getId();

        repo.delete(item);
        repo.flush();
        reindex(cardId);
    }

    private void reindex(Long cardId) {
        List<ChecklistItem> siblings = repo.findByCardIdOrderByPositionAsc(cardId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    private ChecklistItem findOrThrow(Long id) {
        ChecklistItem item = repo.findById(id).orElseThrow(() -> naoEncontrado(id));
        if (!boardAccess.isMember(item.getCard().getList().getBoard().getId())) {
            throw naoEncontrado(id);
        }
        return item;
    }

    private NotFoundException naoEncontrado(Long id) {
        return new NotFoundException("Item de checklist %d não encontrado".formatted(id));
    }
}
