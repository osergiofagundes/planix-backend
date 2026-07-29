package com.sergio.planix.comment;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository repo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;
    private final CurrentUser currentUser;

    public CommentService(CommentRepository repo, CardAccess cardAccess,
                          BoardAccess boardAccess, CurrentUser currentUser) {
        this.repo = repo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByCard(Long cardId) {
        cardAccess.require(cardId);
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(CommentResponse::from).toList();
    }

    public CommentResponse create(Long cardId, CommentRequest req) {
        Card card = cardAccess.require(cardId);
        return CommentResponse.from(
                repo.save(new Comment(card, currentUser.reference(), req.text())));
    }

    public CommentResponse update(Long id, CommentRequest req) {
        Comment comment = findOrThrow(id);
        if (!isAuthor(comment)) {
            throw new ForbiddenException("Só o autor pode editar o próprio comentário");
        }
        comment.setText(req.text());
        return CommentResponse.from(comment);
    }

    public void delete(Long id) {
        Comment comment = findOrThrow(id);
        if (!isAuthor(comment)) {
            boardAccess.requireOwner(comment.getCard().getList().getBoard().getId());
        }
        repo.delete(comment);
    }

    private boolean isAuthor(Comment comment) {
        return comment.getAuthor().getId().equals(currentUser.id());
    }

    private Comment findOrThrow(Long id) {
        Comment comment = repo.findById(id).orElseThrow(() -> naoEncontrado(id));
        if (!boardAccess.isMember(comment.getCard().getList().getBoard().getId())) {
            throw naoEncontrado(id);
        }
        return comment;
    }

    private NotFoundException naoEncontrado(Long id) {
        return new NotFoundException("Comentário %d não encontrado".formatted(id));
    }
}
