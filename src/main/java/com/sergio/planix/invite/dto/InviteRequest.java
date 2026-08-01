package com.sergio.planix.invite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Parâmetros do link de convite. Os dois campos são opcionais — mandar `{}` "
                    + "cria um convite de 1 uso válido por 7 dias.")
public record InviteRequest(
        @Schema(description = "Validade do link em dias. Padrão: 7.", example = "7")
        @Min(1) @Max(30) Integer expiresInDays,

        @Schema(description = "Quantas pessoas podem entrar por este link. Padrão: 1.", example = "1")
        @Min(1) @Max(50) Integer maxUses) {}
