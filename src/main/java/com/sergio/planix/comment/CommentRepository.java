package com.sergio.planix.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByCardIdAndParentIsNullOrderByCreatedAtDesc(Long cardId);

    List<Comment> findByParentIdInOrderByCreatedAtAsc(Collection<Long> parentIds);
}
