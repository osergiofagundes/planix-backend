package com.sergio.planix.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou renomear uma equipe.")
public record TeamRequest(
        @Schema(description = "Nome da equipe", example = "Acme")
        @NotBlank @Size(max = 150) String name,

        @Schema(description = "Descrição livre. Opcional.",
                example = "Todo mundo da Acme, de todos os times.")
        @Size(max = 2000) String description,

        @Schema(description = "Chave do ícone da equipe. Opcional.", example = "building-2")
        @Size(max = 50) String icon
) {}
