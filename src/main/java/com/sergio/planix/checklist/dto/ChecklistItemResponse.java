package com.sergio.planix.checklist.dto;

import com.sergio.planix.checklist.ChecklistItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Um item da checklist de um cartão.")
public record ChecklistItemResponse(
        @Schema(description = "Id do item", example = "200") Long id,
        @Schema(description = "Cartão a que o item pertence", example = "100") Long cardId,
        @Schema(description = "Texto do item", example = "Conferir disponibilidade no registro.br")
        String text,
        @Schema(description = "Se o item está marcado", example = "false") boolean done,
        @Schema(description = "Ordem do item na checklist, começando em 0", example = "0") int position,
        @Schema(description = "Quando o item foi criado", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração no item", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(item.getId(), item.getCard().getId(), item.getText(),
                item.isDone(), item.getPosition(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
