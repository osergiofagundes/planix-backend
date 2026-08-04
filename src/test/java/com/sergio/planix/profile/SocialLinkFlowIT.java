package com.sergio.planix.profile;

import com.sergio.planix.profile.dto.SocialLinkResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLinkFlowIT extends AuthenticatedIntegrationTest {

    @Autowired SocialLinkService socialLinks;
    @Autowired ProfileService profileService;

    @Test
    void upsertNaMesmaRede_substituiEmVezDeDuplicar() {
        socialLinks.upsert(SocialPlatform.LINKEDIN, "https://linkedin.com/in/antigo");
        socialLinks.upsert(SocialPlatform.LINKEDIN, "https://linkedin.com/in/novo");

        assertThat(socialLinks.list())
                .singleElement()
                .extracting(SocialLinkResponse::url)
                .isEqualTo("https://linkedin.com/in/novo");
    }

    @Test
    void cadastraVariasRedes_removeUma_eORestoFica() {
        socialLinks.upsert(SocialPlatform.LINKEDIN, "https://linkedin.com/in/sergio");
        socialLinks.upsert(SocialPlatform.X, "https://x.com/sergio");
        socialLinks.upsert(SocialPlatform.GITHUB, "https://github.com/sergio");

        assertThat(socialLinks.list()).hasSize(3);

        socialLinks.delete(SocialPlatform.X);

        assertThat(socialLinks.list())
                .extracting(SocialLinkResponse::platform)
                .containsExactlyInAnyOrder(SocialPlatform.LINKEDIN, SocialPlatform.GITHUB);

        socialLinks.delete(SocialPlatform.X);   // idempotente: remover de novo não quebra
        assertThat(socialLinks.list()).hasSize(2);
    }

    @Test
    void asRedesAparecemNoPerfil() {
        socialLinks.upsert(SocialPlatform.INSTAGRAM, "https://instagram.com/sergio");

        assertThat(profileService.get().socialLinks())
                .singleElement()
                .extracting(SocialLinkResponse::platform)
                .isEqualTo(SocialPlatform.INSTAGRAM);
    }
}
