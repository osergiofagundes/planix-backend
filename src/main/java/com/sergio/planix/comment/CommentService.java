package com.sergio.planix.comment;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.comment.dto.CommentReactionRequest;
import com.sergio.planix.comment.dto.CommentReactionSummary;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import com.sergio.planix.common.exception.CommentDeletedException;
import com.sergio.planix.common.exception.ForbiddenException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.notification.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class CommentService {

    private final CommentRepository repo;
    private final CommentReactionRepository reactionRepo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;
    private final CurrentUser currentUser;
    private final NotificationPublisher notifications;

    public CommentService(CommentRepository repo, CommentReactionRepository reactionRepo,
                          CardAccess cardAccess, BoardAccess boardAccess, CurrentUser currentUser,
                          NotificationPublisher notifications) {
        this.repo = repo;
        this.reactionRepo = reactionRepo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
        this.currentUser = currentUser;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByCard(Long cardId) {
        cardAccess.require(cardId);

        List<Comment> roots = repo.findByCardIdAndParentIsNullOrderByCreatedAtDesc(cardId);
        if (roots.isEmpty()) {
            return List.of();
        }

        List<Comment> replies = repo.findByParentIdInOrderByCreatedAtAsc(ids(roots));
        Map<Long, List<CommentReactionSummary>> reactions =
                reactionsByComment(ids(Stream.concat(roots.stream(), replies.stream()).toList()));

        Map<Long, List<CommentResponse>> repliesByParent = replies.stream().collect(Collectors.groupingBy(
                reply -> reply.getParent().getId(),
                LinkedHashMap::new,
                Collectors.mapping(reply -> CommentResponse.of(
                        reply, reactions.getOrDefault(reply.getId(), List.of()), List.of()), Collectors.toList())));

        return roots.stream()
                .map(root -> CommentResponse.of(root, reactions.getOrDefault(root.getId(), List.of()),
                        repliesByParent.getOrDefault(root.getId(), List.of())))
                .toList();
    }

    public CommentResponse create(Long cardId, CommentRequest req) {
        Card card = cardAccess.require(cardId);
        Comment comment = repo.save(
                new Comment(card, currentUser.reference(), req.text(), threadRoot(cardId, req.parentId())));

        notifications.cardCommented(card, req.text());
        return CommentResponse.from(comment);
    }

    public CommentResponse update(Long id, CommentRequest req) {
        Comment comment = findAliveOrThrow(id);
        if (!isAuthor(comment)) {
            throw new ForbiddenException("Só o autor pode editar o próprio comentário");
        }
        comment.setText(req.text());
        return CommentResponse.of(comment, summarize(reactionRepo.findByCommentIdOrderByCreatedAtAsc(id)), List.of());
    }

    public void delete(Long id) {
        Comment comment = findOrThrow(id);
        if (!isAuthor(comment)) {
            boardAccess.requireManager(comment.getCard().getList().getBoard().getId());
        }

        comment.setDeletedAt(OffsetDateTime.now());
        comment.setText("");
    }

    public List<CommentReactionSummary> toggleReaction(Long commentId, CommentReactionRequest req) {
        Comment comment = findOrThrow(commentId);

        String emoji = req.emoji().trim();
        Optional<CommentReaction> existing =
                reactionRepo.findByCommentIdAndUserIdAndEmoji(commentId, currentUser.id(), emoji);

        if (existing.isPresent()) {
            reactionRepo.delete(existing.get());
        } else {
            reactionRepo.save(new CommentReaction(comment, currentUser.reference(), emoji));
        }

        reactionRepo.flush();
        return summarize(reactionRepo.findByCommentIdOrderByCreatedAtAsc(commentId));
    }

    private Comment threadRoot(Long cardId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        Comment parent = findAliveOrThrow(parentId);
        if (!parent.getCard().getId().equals(cardId)) {
            throw naoEncontrado(parentId);
        }
        return parent.getParent() == null ? parent : parent.getParent();
    }

    private Map<Long, List<CommentReactionSummary>> reactionsByComment(Collection<Long> commentIds) {
        return reactionRepo.findByCommentIdInOrderByCreatedAtAsc(commentIds).stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getComment().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> summarize(entry.getValue())));
    }

    private List<CommentReactionSummary> summarize(List<CommentReaction> reactions) {
        Long userId = currentUser.id();
        return reactions.stream()
                .collect(Collectors.groupingBy(CommentReaction::getEmoji, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> CommentReactionSummary.of(entry.getKey(), entry.getValue(), userId))
                .toList();
    }

    private List<Long> ids(List<Comment> comments) {
        return comments.stream().map(Comment::getId).toList();
    }

    private boolean isAuthor(Comment comment) {
        return comment.getAuthor().getId().equals(currentUser.id());
    }

    private Comment findAliveOrThrow(Long id) {
        Comment comment = findOrThrow(id);
        if (comment.isDeleted()) {
            throw new CommentDeletedException("Comentário %d foi excluído".formatted(id));
        }
        return comment;
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
