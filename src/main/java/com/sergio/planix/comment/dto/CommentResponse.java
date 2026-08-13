package com.sergio.planix.comment.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.comment.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Um comentário de um cartão.")
public record CommentResponse(
        @Schema(description = "Id do comentário", example = "300") Long id,
        @Schema(description = "Cartão comentado", example = "100") Long cardId,
        @Schema(description = "Comentário respondido. `null` em um comentário raiz.",
                example = "299") Long parentId,
        @Schema(description = "Conteúdo do comentário. `null` se foi excluído.",
                example = "O registro.br cobra por 2 anos no mínimo, dá R$ 80.")
        String text,
        @Schema(description = "Quem escreveu") UserSummary author,
        @Schema(description = "Se foi excluído. A linha continua na listagem, sem o texto.",
                example = "false")
        boolean deleted,
        @Schema(description = "Se o texto foi alterado depois de escrito. Sempre `false` no excluído.",
                example = "false")
        boolean edited,
        @Schema(description = "Reações agrupadas por emoji") List<CommentReactionSummary> reactions,
        @Schema(description = "Respostas, da mais antiga para a mais nova. Sempre vazia dentro de uma resposta.")
        List<CommentResponse> replies,
        @Schema(description = "Quando foi escrito", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última edição", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {

    public static CommentResponse from(Comment comment) {
        return of(comment, List.of(), List.of());
    }

    public static CommentResponse of(Comment comment,
                                     List<CommentReactionSummary> reactions,
                                     List<CommentResponse> replies) {
        boolean deleted = comment.isDeleted();
        return new CommentResponse(
                comment.getId(),
                comment.getCard().getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                deleted ? null : comment.getText(),
                UserSummary.from(comment.getAuthor()),
                deleted,
                !deleted && comment.getUpdatedAt().isAfter(comment.getCreatedAt()),
                reactions,
                replies,
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
