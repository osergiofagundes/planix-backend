package com.sergio.planix.link.dto;

import com.sergio.planix.link.CardLink;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Um link anexado a um cartão.")
public record CardLinkResponse(
        @Schema(description = "Id do link", example = "400") Long id,
        @Schema(description = "Cartão a que o link pertence", example = "100") Long cardId,
        @Schema(description = "Endereço", example = "https://registro.br/dominio/") String url,
        @Schema(description = "Rótulo do link", example = "Consulta de domínios .br") String title,
        @Schema(description = "Quando o link foi anexado", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração no link", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static CardLinkResponse from(CardLink link) {
        return new CardLinkResponse(link.getId(), link.getCard().getId(), link.getUrl(),
                link.getTitle(), link.getCreatedAt(), link.getUpdatedAt());
    }
}
