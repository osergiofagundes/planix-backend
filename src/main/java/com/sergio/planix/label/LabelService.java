package com.sergio.planix.label;

import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.common.LabelNameAlreadyUsedException;
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
    private final BoardRepository boardRepo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;

    public LabelService(LabelRepository labelRepo, BoardRepository boardRepo,
                        CardAccess cardAccess, BoardAccess boardAccess) {
        this.labelRepo = labelRepo;
        this.boardRepo = boardRepo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> listByBoard(Long boardId) {
        boardAccess.requireMember(boardId);
        return labelRepo.findByBoardIdOrderByNameAsc(boardId).stream().map(LabelResponse::from).toList();
    }

    public LabelResponse create(Long boardId, LabelRequest req) {
        boardAccess.requireMember(boardId);

        if (labelRepo.existsByBoardIdAndName(boardId, req.name())) {
            throw new LabelNameAlreadyUsedException(
                    "O quadro já tem uma etiqueta chamada \"%s\"".formatted(req.name()));
        }

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
        Card card = cardAccess.require(cardId);
        Label label = findOrThrow(labelId);
        card.getLabels().add(label);
    }

    public void detach(Long cardId, Long labelId) {
        Card card = cardAccess.require(cardId);
        Label label = findOrThrow(labelId);
        card.getLabels().removeIf(l -> l.getId().equals(label.getId()));
    }

    private Label findOrThrow(Long id) {
        Label label = labelRepo.findById(id).orElseThrow(() -> naoEncontrada(id));
        if (!boardAccess.isMember(label.getBoard().getId())) {
            throw naoEncontrada(id);
        }
        return label;
    }

    private NotFoundException naoEncontrada(Long id) {
        return new NotFoundException("Etiqueta %d não encontrada".formatted(id));
    }
}
