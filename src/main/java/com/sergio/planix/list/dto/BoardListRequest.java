package com.sergio.planix.list.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou renomear uma lista.")
public record BoardListRequest(
        @Schema(description = "Nome da lista, como apareceria no topo da coluna", example = "A Fazer")
        @NotBlank @Size(max = 150) String name
) {}
