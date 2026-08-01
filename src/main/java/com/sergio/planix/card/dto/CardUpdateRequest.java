package com.sergio.planix.card.dto;

import com.sergio.planix.card.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "Substitui os campos editáveis do cartão. Campos omitidos são gravados como "
                    + "nulos — é um `PUT`, não um `PATCH`.")
public record CardUpdateRequest(
        @Schema(description = "Título do cartão", example = "Comprar domínio")
        @NotBlank @Size(max = 200) String title,

        @Schema(description = "Descrição livre. Opcional.",
                example = "Conferir se planix.dev está disponível antes de fechar.")
        @Size(max = 5000) String description,

        @Schema(description = "Prazo do cartão, com fuso. Opcional.",
                example = "2026-08-20T18:00:00-03:00")
        OffsetDateTime dueDate,

        @Schema(description = "Prioridade. Opcional; quando omitida, o cartão fica em `NONE`.",
                example = "HIGH")
        Priority priority
) {}
