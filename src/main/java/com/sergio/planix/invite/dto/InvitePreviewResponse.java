package com.sergio.planix.invite.dto;

import com.sergio.planix.auth.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "O que dá para saber sobre um convite antes de aceitá-lo. É o suficiente "
                    + "para uma tela de \"Você foi convidado para...\" e nada além disso.")
public record InvitePreviewResponse(
        @Schema(description = "Nome do quadro para o qual o convite leva", example = "Lançamento do site")
        String boardName,
        @Schema(description = "Quem criou o convite") UserSummary invitedBy,
        @Schema(description = "Quando o link deixa de valer", example = "2026-08-08T14:32:10.123-03:00")
        OffsetDateTime expiresAt
) {}
