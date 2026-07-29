package com.sergio.planix.label;

import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.label.dto.LabelRequest;
import com.sergio.planix.label.dto.LabelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LabelService {

    private final LabelRepository labelRepo;
    private final CardRepository cardRepo;
    private final BoardRepository boardRepo;

    public LabelService(LabelRepository labelRepo, CardRepository cardRepo, BoardRepository boardRepo) {
        this.labelRepo = labelRepo;
        this.cardRepo = cardRepo;
        this.boardRepo = boardRepo;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> listByBoard(Long boardId) {
        if (!boardRepo.existsById(boardId)) {
            throw new NotFoundException("Quadro %d não encontrado".formatted(boardId));
        }
        return labelRepo.findByBoardIdOrderByNameAsc(boardId).stream().map(LabelResponse::from).toList();
    }

    public LabelResponse create(Long boardId, LabelRequest req) {
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(boardId)));
        return LabelResponse.from(labelRepo.save(new Label(board, req.name(), req.color())));
    }

    public LabelResponse update(Long id, LabelRequest req) {
        Label label = findOrThrow(id);
        label.setName(req.name());
        label.setColor(req.color());
        return LabelResponse.from(label);
    }

    public void delete(Long id) {
        labelRepo.delete(findOrThrow(id));
    }

    public void attach(Long cardId, Long labelId) {
        Card card = findCardOrThrow(cardId);
        Label label = findOrThrow(labelId);
        card.getLabels().add(label);
    }

    public void detach(Long cardId, Long labelId) {
        Card card = findCardOrThrow(cardId);
        card.getLabels().removeIf(l -> l.getId().equals(labelId));
    }

    private Label findOrThrow(Long id) {
        return labelRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Etiqueta %d não encontrada".formatted(id)));
    }

    private Card findCardOrThrow(Long id) {
        return cardRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(id)));
    }
}
