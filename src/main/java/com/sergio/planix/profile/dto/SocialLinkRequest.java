package com.sergio.planix.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "URL de uma rede social. A rede em si vai na URL do endpoint.")
public record SocialLinkRequest(

        @Schema(description = "Endereço do perfil naquela rede",
                example = "https://linkedin.com/in/sergiofagundes")
        @NotBlank @Size(max = 255)
        @Pattern(regexp = "^https?://.+", message = "deve começar com http:// ou https://")
        String url
) {}
