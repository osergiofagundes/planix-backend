package com.sergio.planix.auth.dto;

import com.sergio.planix.auth.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados da conta autenticada.")
public record UserResponse(
        @Schema(description = "Id do usuário", example = "1") Long id,
        @Schema(description = "Nome de exibição", example = "Sérgio Fagundes") String name,
        @Schema(description = "E-mail da conta", example = "sergio@planix.dev") String email
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
