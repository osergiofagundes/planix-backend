package com.sergio.planix.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Conclui ou reabre o cartão.")
public record CardCompleteRequest(
        @Schema(description = "`true` conclui e grava o `completedAt`; `false` reabre e o limpa.",
                example = "true")
        @NotNull Boolean completed
) {}
