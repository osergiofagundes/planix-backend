package com.sergio.planix.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "Corpo padrão de qualquer resposta de erro da API.")
public record ApiError(
        @Schema(description = "Quando o erro aconteceu", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime timestamp,

        @Schema(description = "Código HTTP, repetido no corpo para facilitar o log do cliente",
                example = "404")
        int status,

        @Schema(description = "Nome do status HTTP", example = "Not Found")
        String error,

        @Schema(description = "Explicação legível do que deu errado",
                example = "Quadro 42 não encontrado")
        String message,

        @Schema(description = "Caminho que foi chamado", example = "/api/boards/42")
        String path,

        @Schema(description = "Motivo de cada campo recusado, um por chave. Só aparece nos erros de "
                            + "validação (400); nos demais é nulo.")
        Map<String, String> fieldErrors
) {}
