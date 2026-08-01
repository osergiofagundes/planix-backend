package com.sergio.planix.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nova posição de um item dentro do seu grupo.")
public record MoveRequest(
        @Schema(description = "Índice de destino, começando em 0. Os vizinhos se acomodam sozinhos — "
                            + "não é preciso mandar as posições deles.",
                example = "2")
        @NotNull Integer position
) {}
