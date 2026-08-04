package com.sergio.planix.profile.dto;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.dto.AvatarUrl;
import com.sergio.planix.profile.SocialLink;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Perfil completo da conta autenticada.")
public record ProfileResponse(
        @Schema(description = "Id do usuário", example = "1") Long id,
        @Schema(description = "Nome de exibição", example = "Sérgio Fagundes") String name,
        @Schema(description = "E-mail da conta", example = "sergio@planix.dev") String email,
        @Schema(description = "Data de nascimento", example = "1990-04-17") LocalDate birthDate,
        @Schema(description = "Telefone", example = "+55 51 99999-0000") String phone,
        @Schema(description = "Biografia") String bio,

        @Schema(description = "Onde baixar a foto de perfil. `null` quando não há foto.",
                example = "/api/users/1/avatar") String avatarUrl,

        @Schema(description = "Endereço") AddressResponse address,
        @Schema(description = "Redes sociais cadastradas") List<SocialLinkResponse> socialLinks
) {

    public static ProfileResponse from(User user, List<SocialLink> links) {
        return new ProfileResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getBirthDate(), user.getPhone(), user.getBio(),
                AvatarUrl.of(user),
                AddressResponse.from(user.getAddress()),
                links.stream().map(SocialLinkResponse::from).toList());
    }
}
