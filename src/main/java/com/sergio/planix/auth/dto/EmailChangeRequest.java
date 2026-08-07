package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Troca do e-mail de login. A senha atual autoriza a operação.")
public record EmailChangeRequest(

        @Schema(description = "O novo e-mail. Precisa estar livre e vira o novo identificador do login.",
                example = "sergio.novo@planix.dev")
        @NotBlank @Email @Size(max = 255) String newEmail,

        @Schema(description = "Senha atual da conta, em texto claro", example = "umaSenhaBoa123")
        @NotBlank String currentPassword
) {}
