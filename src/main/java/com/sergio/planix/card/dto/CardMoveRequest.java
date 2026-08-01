package com.sergio.planix.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Para onde o cartão vai. Para só reordenar na lista atual, repita o id dela "
                    + "em `targetListId`.")
public record CardMoveRequest(
        @Schema(description = "Lista de destino. Precisa ser do **mesmo quadro** — do contrário a "
                            + "resposta é 409.",
                example = "11")
        @NotNull Long targetListId,

        @Schema(description = "Índice de destino dentro da lista, começando em 0", example = "0")
        @NotNull Integer position
) {}
