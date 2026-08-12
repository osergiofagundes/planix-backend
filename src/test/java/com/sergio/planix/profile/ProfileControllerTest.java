package com.sergio.planix.profile;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.common.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ProfileService service;
    @MockitoBean SocialLinkService socialLinks;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void nascimentoNoFuturo_retorna400ComFieldErrors() throws Exception {
        mvc.perform(put("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {"name": "Sergio", "birthDate": "2999-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fieldErrors.birthDate").value("deve ser uma data no passado"));
    }

    @Test
    void nomeEmBranco_retorna400() throws Exception {
        mvc.perform(put("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {"name": "   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void avatarQueNaoEImagem_retorna415ComCorpoApiError() throws Exception {
        when(service.uploadAvatar(any(MultipartFile.class)))
                .thenThrow(new UnsupportedFileTypeException("A foto de perfil precisa ser JPEG, PNG ou WebP"));

        MockMultipartFile texto = new MockMultipartFile("file", "curriculo.txt",
                "text/plain", "não sou uma foto".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/me/avatar").file(texto))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.path").value("/api/me/avatar"))
                .andExpect(jsonPath("$.message").value("A foto de perfil precisa ser JPEG, PNG ou WebP"));
    }

    @Test
    void redeSocialDesconhecida_retorna400ListandoAsAceitas() throws Exception {
        mvc.perform(delete("/api/me/social-links/ORKUT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("LINKEDIN")));
    }
}
