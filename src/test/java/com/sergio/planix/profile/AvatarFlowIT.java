package com.sergio.planix.profile;

import com.sergio.planix.common.UnsupportedFileTypeException;
import com.sergio.planix.profile.dto.ProfileResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarFlowIT extends AuthenticatedIntegrationTest {

    @Autowired ProfileService profileService;
    @Autowired Path uploadDir;

    private MockMultipartFile imagem(String nome, String conteudo) {
        return new MockMultipartFile("file", nome, "image/jpeg",
                conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private String caminhoNoBanco() {
        return userRepo.findById(usuarioLogado.getId()).orElseThrow().getAvatarPath();
    }

    @Test
    void trocarAFotoApagaAAnterior_eRemoverLimpaTudo() throws Exception {
        ProfileResponse comFoto = profileService.uploadAvatar(imagem("eu.jpg", "primeira"));

        assertThat(comFoto.avatarUrl())
                .isEqualTo("/api/users/" + usuarioLogado.getId() + "/avatar");

        String primeiro = caminhoNoBanco();
        assertThat(primeiro).startsWith("profile_pictures/").endsWith(".jpg");
        assertThat(Files.exists(uploadDir.resolve(primeiro))).isTrue();

        profileService.uploadAvatar(imagem("outra.jpg", "segunda"));

        String segundo = caminhoNoBanco();
        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(Files.readString(uploadDir.resolve(segundo), StandardCharsets.UTF_8))
                .isEqualTo("segunda");
        assertThat(Files.exists(uploadDir.resolve(primeiro)))
                .as("a foto antiga não pode ficar órfã no disco").isFalse();

        profileService.deleteAvatar();

        assertThat(Files.exists(uploadDir.resolve(segundo))).isFalse();
        assertThat(caminhoNoBanco()).isNull();
        assertThat(profileService.get().avatarUrl()).isNull();
    }

    @Test
    void arquivoQueNaoEImagem_eRecusado() {
        MockMultipartFile texto = new MockMultipartFile("file", "curriculo.txt",
                "text/plain", "não sou uma foto".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> profileService.uploadAvatar(texto))
                .isInstanceOf(UnsupportedFileTypeException.class);

        assertThat(caminhoNoBanco()).isNull();
    }

    @Test
    void imagemAcimaDe2MB_eRecusada() {
        MockMultipartFile grande = new MockMultipartFile("file", "enorme.png", "image/png",
                new byte[(int) ProfileService.MAX_AVATAR_BYTES + 1]);

        assertThatThrownBy(() -> profileService.uploadAvatar(grande))
                .isInstanceOf(MaxUploadSizeExceededException.class);

        assertThat(caminhoNoBanco()).isNull();
    }

    @Test
    void avatarDeUsuarioSemFoto_da404() {
        assertThatThrownBy(() -> profileService.avatarOf(usuarioLogado.getId()))
                .isInstanceOf(com.sergio.planix.common.NotFoundException.class);
    }
}
