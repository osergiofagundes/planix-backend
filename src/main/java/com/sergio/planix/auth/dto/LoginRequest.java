package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso.")
public record LoginRequest(
        @Schema(description = "E-mail cadastrado", example = "sergio@planix.dev")
        @NotBlank String email,

        @Schema(description = "Senha da conta", example = "umaSenhaBoa123")
        @NotBlank String password
) {}
