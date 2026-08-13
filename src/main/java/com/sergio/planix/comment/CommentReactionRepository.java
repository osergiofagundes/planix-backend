package com.sergio.planix.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    List<CommentReaction> findByCommentIdInOrderByCreatedAtAsc(Collection<Long> commentIds);

    List<CommentReaction> findByCommentIdOrderByCreatedAtAsc(Long commentId);

    Optional<CommentReaction> findByCommentIdAndUserIdAndEmoji(Long commentId, Long userId, String emoji);
}
