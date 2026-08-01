package com.sergio.planix.label.dto;

import com.sergio.planix.label.Label;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Uma etiqueta. Pertence ao quadro e é reutilizável em vários cartões.")
public record LabelResponse(
        @Schema(description = "Id da etiqueta", example = "5") Long id,
        @Schema(description = "Quadro a que a etiqueta pertence", example = "1") Long boardId,
        @Schema(description = "Nome da etiqueta", example = "Urgente") String name,
        @Schema(description = "Cor da etiqueta", example = "#E53935") String color,
        @Schema(description = "Quando a etiqueta foi criada", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração na etiqueta", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getBoard().getId(), label.getName(),
                label.getColor(), label.getCreatedAt(), label.getUpdatedAt());
    }
}
