package com.sergio.planix.checklist;

import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.checklist.dto.ChecklistItemRequest;
import com.sergio.planix.checklist.dto.ChecklistItemResponse;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChecklistItemService {

    private final ChecklistItemRepository repo;
    private final CardRepository cardRepo;

    public ChecklistItemService(ChecklistItemRepository repo, CardRepository cardRepo) {
        this.repo = repo;
        this.cardRepo = cardRepo;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> listByCard(Long cardId) {
        if (!cardRepo.existsById(cardId)) {
            throw new NotFoundException("Cartão %d não encontrado".formatted(cardId));
        }
        return repo.findByCardIdOrderByPositionAsc(cardId).stream()
                .map(ChecklistItemResponse::from).toList();
    }

    public ChecklistItemResponse create(Long cardId, ChecklistItemRequest req) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(cardId)));
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
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Item de checklist %d não encontrado".formatted(id)));
    }
}
