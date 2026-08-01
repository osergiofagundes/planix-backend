package com.sergio.planix.checklist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "O texto de um item da checklist. Marcar/desmarcar é outro endpoint (`/toggle`).")
public record ChecklistItemRequest(
        @Schema(description = "Texto do item", example = "Conferir disponibilidade no registro.br")
        @NotBlank @Size(max = 300) String text
) {}
