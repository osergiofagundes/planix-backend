package com.sergio.planix.card.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.Priority;
import com.sergio.planix.label.dto.LabelResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = """
        Um cartão, com etiquetas e responsáveis embutidos. Checklist, comentários, links, anexos e
        histórico ficam em endpoints próprios — não vêm aqui.""")
public record CardResponse(
        @Schema(description = "Id do cartão", example = "100") Long id,
        @Schema(description = "Lista em que o cartão está agora", example = "10") Long listId,
        @Schema(description = "Título do cartão", example = "Comprar domínio") String title,
        @Schema(description = "Descrição livre", example = "Conferir se planix.dev está disponível antes de fechar.")
        String description,
        @Schema(description = "Prazo do cartão", example = "2026-08-20T18:00:00-03:00")
        OffsetDateTime dueDate,
        @Schema(description = "Prioridade do cartão", example = "HIGH") Priority priority,
        @Schema(description = "Ordem do cartão dentro da lista, começando em 0", example = "0") int position,
        @Schema(description = "Se o cartão está concluído", example = "false") boolean completed,
        @Schema(description = "Quando foi concluído. Nulo enquanto o cartão estiver aberto.",
                example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime completedAt,
        @Schema(description = "Etiquetas aplicadas, em ordem alfabética") List<LabelResponse> labels,
        @Schema(description = "Quem é responsável pelo cartão, em ordem alfabética") List<UserSummary> assignees,
        @Schema(description = "Quando o cartão foi criado", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração no cartão", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static CardResponse from(Card card) {
        List<LabelResponse> labels = card.getLabels().stream()
                .map(LabelResponse::from)
                .sorted(Comparator.comparing(LabelResponse::name))
                .toList();
        List<UserSummary> assignees = card.getAssignees().stream()
                .map(UserSummary::from)
                .sorted(Comparator.comparing(UserSummary::name))
                .toList();
        return new CardResponse(card.getId(), card.getList().getId(), card.getTitle(),
                card.getDescription(), card.getDueDate(), card.getPriority(), card.getPosition(),
                card.isCompleted(), card.getCompletedAt(), labels, assignees,
                card.getCreatedAt(), card.getUpdatedAt());
    }
}
