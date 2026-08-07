package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Troca da senha. A senha atual autoriza a operação.")
public record PasswordChangeRequest(

        @Schema(description = "Senha atual da conta, em texto claro", example = "umaSenhaBoa123")
        @NotBlank String currentPassword,

        @Schema(description = "A nova senha. Precisa ser diferente da atual.",
                example = "umaSenhaAindaMelhor456")
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
