package com.sergio.planix.invite.dto;

import com.sergio.planix.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Parâmetros do link de convite. Os três campos são opcionais — mandar `{}` "
                    + "cria um convite de 1 uso, válido por 7 dias, que entra como `MEMBER`.")
public record InviteRequest(
        @Schema(description = "Validade do link em dias. Padrão: 7.", example = "7")
        @Min(1) @Max(30) Integer expiresInDays,

        @Schema(description = "Quantas pessoas podem entrar por este link. Padrão: 1.", example = "1")
        @Min(1) @Max(50) Integer maxUses,

        @Schema(description = "Com que papel quem aceitar entra na equipe: `ADMIN` ou `MEMBER`. "
                            + "Padrão: `MEMBER`.", example = "MEMBER")
        TeamRole role) {}
