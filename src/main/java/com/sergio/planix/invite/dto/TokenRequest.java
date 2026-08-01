package com.sergio.planix.invite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "O token de um convite, para espiar ou aceitar.")
public record TokenRequest(
        @Schema(description = "O token recebido no link do convite",
                example = "b17f4d2c9a0e83516d7b2f9c4e1a0d38")
        @NotBlank String token
) {}
