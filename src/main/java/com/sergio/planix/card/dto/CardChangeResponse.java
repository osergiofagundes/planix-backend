package com.sergio.planix.card.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.card.CardChange;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Uma alteração registrada no cartão. É um diff campo a campo: uma atualização "
                    + "que mexeu em três campos gera três linhas.")
public record CardChangeResponse(
        @Schema(description = "Campo alterado", example = "priority") String field,
        @Schema(description = "Valor antes da alteração. Nulo quando o campo estava vazio.",
                example = "NONE")
        String oldValue,
        @Schema(description = "Valor depois da alteração. Nulo quando o campo foi limpo.",
                example = "HIGH")
        String newValue,
        @Schema(description = "Quem fez a alteração") UserSummary author,
        @Schema(description = "Quando a alteração aconteceu", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime changedAt
) {
    public static CardChangeResponse from(CardChange change) {
        return new CardChangeResponse(change.getField(), change.getOldValue(),
                change.getNewValue(), UserSummary.from(change.getAuthor()), change.getChangedAt());
    }
}
