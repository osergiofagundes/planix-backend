package com.sergio.planix.auth.dto;

import com.sergio.planix.auth.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Referência resumida a um usuário. Sem e-mail, de propósito.")
public record UserSummary(
        @Schema(description = "Id do usuário", example = "1") Long id,
        @Schema(description = "Nome de exibição", example = "Sérgio Fagundes") String name
) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName());
    }
}
