package com.sergio.planix.profile.dto;

import com.sergio.planix.profile.SocialLink;
import com.sergio.planix.profile.SocialPlatform;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma rede social do perfil.")
public record SocialLinkResponse(
        @Schema(description = "A rede", example = "LINKEDIN") SocialPlatform platform,
        @Schema(description = "URL do perfil", example = "https://linkedin.com/in/sergiofagundes") String url
) {

    public static SocialLinkResponse from(SocialLink link) {
        return new SocialLinkResponse(link.getPlatform(), link.getUrl());
    }
}
