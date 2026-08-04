package com.sergio.planix.profile;

import com.sergio.planix.profile.dto.AddressRequest;
import com.sergio.planix.profile.dto.ProfileRequest;
import com.sergio.planix.profile.dto.ProfileResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileFlowIT extends AuthenticatedIntegrationTest {

    @Autowired ProfileService profileService;

    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 4, 17);

    @Test
    void preencheOPerfilInteiro_eLeDeVolta() {
        ProfileResponse salvo = profileService.update(new ProfileRequest(
                "Sérgio Fagundes", NASCIMENTO, "+55 51 99999-0000", "Backend em Java.",
                new AddressRequest("Rua das Acácias", "1024", "Apto 32B",
                                   "Porto Alegre", "rs", "90000-000")));

        assertThat(salvo.name()).isEqualTo("Sérgio Fagundes");
        assertThat(salvo.birthDate()).isEqualTo(NASCIMENTO);
        assertThat(salvo.phone()).isEqualTo("+55 51 99999-0000");
        assertThat(salvo.bio()).isEqualTo("Backend em Java.");
        assertThat(salvo.address().city()).isEqualTo("Porto Alegre");
        assertThat(salvo.address().state()).isEqualTo("RS");   // veio "rs", foi guardado maiúsculo
        assertThat(salvo.avatarUrl()).isNull();
        assertThat(salvo.socialLinks()).isEmpty();

        assertThat(profileService.get()).isEqualTo(salvo);
    }

    @Test
    void putComCamposNulos_apagaOQueEstavaLa() {
        profileService.update(new ProfileRequest(
                "Sérgio Fagundes", NASCIMENTO, "+55 51 99999-0000", "Backend em Java.",
                new AddressRequest("Rua das Acácias", "1024", null, "Porto Alegre", "RS", "90000-000")));

        ProfileResponse limpo = profileService.update(
                new ProfileRequest("Sérgio", null, null, null, null));

        assertThat(limpo.name()).isEqualTo("Sérgio");
        assertThat(limpo.birthDate()).isNull();
        assertThat(limpo.phone()).isNull();
        assertThat(limpo.bio()).isNull();
        assertThat(limpo.address()).isNull();

        assertThat(profileService.get().address()).isNull();
    }

    @Test
    void camposSoComEspacosEmBranco_viramNulo() {
        ProfileResponse salvo = profileService.update(new ProfileRequest(
                "Sérgio", null, "  ", "   ",
                new AddressRequest("  ", "", null, "  ", null, null)));

        assertThat(salvo.phone()).isNull();
        assertThat(salvo.bio()).isNull();
        assertThat(salvo.address()).isNull();
    }
}
