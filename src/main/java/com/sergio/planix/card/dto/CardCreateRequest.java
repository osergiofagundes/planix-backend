package com.sergio.planix.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados mínimos para criar um cartão. Descrição, prazo e prioridade entram "
                    + "depois, por `PUT /api/cards/{id}`.")
public record CardCreateRequest(
        @Schema(description = "Título do cartão", example = "Comprar domínio")
        @NotBlank @Size(max = 200) String title
) {}
