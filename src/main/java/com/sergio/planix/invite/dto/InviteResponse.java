package com.sergio.planix.invite.dto;

import com.sergio.planix.invite.TeamInvite;
import com.sergio.planix.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "O estado de um convite. Repare que o token não vem aqui — ele só existiu na "
                    + "resposta da criação.")
public record InviteResponse(
        @Schema(description = "Id do convite", example = "600") Long id,
        @Schema(description = "Papel com que quem aceitar entra na equipe") TeamRole role,
        @Schema(description = "Quantas pessoas já entraram por este link", example = "0") int uses,
        @Schema(description = "Limite de usos", example = "1") int maxUses,
        @Schema(description = "Quando o link deixa de valer", example = "2026-08-08T14:32:10.123-03:00")
        OffsetDateTime expiresAt,
        @Schema(description = "Quando foi revogado. Nulo enquanto o convite estiver ativo.",
                example = "2026-08-02T10:05:00.000-03:00")
        OffsetDateTime revokedAt,
        @Schema(description = "Quando o convite foi criado", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime createdAt
) {

    public static InviteResponse from(TeamInvite invite) {
        return new InviteResponse(invite.getId(), invite.getRole(), invite.getUses(),
                invite.getMaxUses(), invite.getExpiresAt(), invite.getRevokedAt(),
                invite.getCreatedAt());
    }
}
