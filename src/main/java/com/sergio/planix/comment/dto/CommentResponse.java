package com.sergio.planix.comment.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.comment.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Um comentário de um cartão.")
public record CommentResponse(
        @Schema(description = "Id do comentário", example = "300") Long id,
        @Schema(description = "Cartão comentado", example = "100") Long cardId,
        @Schema(description = "Conteúdo do comentário",
                example = "O registro.br cobra por 2 anos no mínimo, dá R$ 80.")
        String text,
        @Schema(description = "Quem escreveu") UserSummary author,
        @Schema(description = "Quando foi escrito", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última edição", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getCard().getId(), comment.getText(),
                UserSummary.from(comment.getAuthor()),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
