package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar uma conta.")
public record RegisterRequest(
        @Schema(description = "Nome de exibição", example = "Sérgio Fagundes")
        @NotBlank @Size(max = 150) String name,

        @Schema(description = "E-mail, único no sistema. É o identificador do login.",
                example = "sergio@planix.dev")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "Senha em texto claro. É guardada apenas como hash BCrypt.",
                example = "umaSenhaBoa123")
        @NotBlank @Size(min = 8, max = 72) String password
) {}
