package com.sergio.planix.auth;

import com.jayway.jsonpath.JsonPath;
import com.sergio.planix.support.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountHttpIT extends HttpIntegrationTest {

    private static final String SENHA = "senha-de-teste";
    private static final String SENHA_NOVA = "senha-nova-123";

    @Test
    void trocarASenha_devolveUmParDeTokensQueFunciona() throws Exception {
        String registro = registrar(emailUnico(), SENHA);

        String trocada = trocarSenha(JsonPath.read(registro, "$.accessToken"), SENHA, SENHA_NOVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andReturn().getResponse().getContentAsString();

        renovar(JsonPath.read(trocada, "$.refreshToken")).andExpect(status().isOk());
    }

    @Test
    void trocarASenha_derrubaAsSessoesAnteriores() throws Exception {
        String registro = registrar(emailUnico(), SENHA);
        String refreshAntigo = JsonPath.read(registro, "$.refreshToken");

        trocarSenha(JsonPath.read(registro, "$.accessToken"), SENHA, SENHA_NOVA)
                .andExpect(status().isOk());

        renovar(refreshAntigo).andExpect(status().isUnauthorized());
    }

    @Test
    void depoisDeTrocarASenha_soAnovaEntra() throws Exception {
        String email = emailUnico();
        String registro = registrar(email, SENHA);

        trocarSenha(JsonPath.read(registro, "$.accessToken"), SENHA, SENHA_NOVA)
                .andExpect(status().isOk());

        entrar(email, SENHA_NOVA).andExpect(status().isOk());
        entrar(email, SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaAtualErrada_retorna400ApontandoOcampo() throws Exception {
        String registro = registrar(emailUnico(), SENHA);

        trocarSenha(JsonPath.read(registro, "$.accessToken"), "chutei-errado", SENHA_NOVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
    }

    @Test
    void senhaNovaIgualAatual_retorna400ApontandoOcampo() throws Exception {
        String registro = registrar(emailUnico(), SENHA);

        trocarSenha(JsonPath.read(registro, "$.accessToken"), SENHA, SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void senhaNovaCurtaDemais_retorna400() throws Exception {
        String registro = registrar(emailUnico(), SENHA);

        trocarSenha(JsonPath.read(registro, "$.accessToken"), SENHA, "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void semToken_naoTrocaSenha() throws Exception {
        mvc.perform(put("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"currentPassword":"%s","newPassword":"%s"}
                                 """.formatted(SENHA, SENHA_NOVA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void depoisDeTrocarOEmail_ologinUsaOenderecoNovo() throws Exception {
        String antigo = emailUnico();
        String novo = emailUnico();
        String registro = registrar(antigo, SENHA);

        trocarEmail(JsonPath.read(registro, "$.accessToken"), novo, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        entrar(novo, SENHA).andExpect(status().isOk());
        entrar(antigo, SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void trocarParaOemailDeOutraConta_retorna409() throws Exception {
        String ocupado = emailUnico();
        registrar(ocupado, SENHA);
        String registro = registrar(emailUnico(), SENHA);

        trocarEmail(JsonPath.read(registro, "$.accessToken"), ocupado, SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void trocarEmail_comSenhaAtualErrada_retorna400ApontandoOcampo() throws Exception {
        String registro = registrar(emailUnico(), SENHA);

        trocarEmail(JsonPath.read(registro, "$.accessToken"), emailUnico(), "chutei-errado")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
    }

    private ResultActions trocarSenha(String accessToken, String atual, String nova) throws Exception {
        return mvc.perform(put("/api/me/password").with(comToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"currentPassword":"%s","newPassword":"%s"}
                         """.formatted(atual, nova)));
    }

    private ResultActions trocarEmail(String accessToken, String novoEmail, String senhaAtual)
            throws Exception {
        return mvc.perform(put("/api/me/email").with(comToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"newEmail":"%s","currentPassword":"%s"}
                         """.formatted(novoEmail, senhaAtual)));
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"email":"%s","password":"%s"}
                         """.formatted(email, senha)));
    }

    private ResultActions renovar(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {"refreshToken":"%s"}
                         """.formatted(refreshToken)));
    }
}
