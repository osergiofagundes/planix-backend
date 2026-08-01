package com.sergio.planix.invite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "O convite recém-criado. **Única** resposta em que o token aparece em texto "
                    + "claro — o banco guarda só o hash.")
public record InviteCreatedResponse(
        @Schema(description = "Id do convite, usado para revogá-lo", example = "600") Long id,
        @Schema(description = "O token do convite. Guarde agora: não há como recuperá-lo depois.",
                example = "b17f4d2c9a0e83516d7b2f9c4e1a0d38")
        String token,
        @Schema(description = "Quando o link deixa de valer", example = "2026-08-08T14:32:10.123-03:00")
        OffsetDateTime expiresAt,
        @Schema(description = "Quantas pessoas podem entrar por este link", example = "1") int maxUses
) {}
