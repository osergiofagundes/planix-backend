package com.sergio.planix.comment.dto;

import com.sergio.planix.comment.Comment;

import java.time.OffsetDateTime;

public record CommentResponse(
        Long id, Long cardId, String text,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getCard().getId(), comment.getText(),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
