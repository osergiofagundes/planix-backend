package com.sergio.planix.auth;

import com.jayway.jsonpath.JsonPath;
import com.sergio.planix.support.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O lado HTTP da autenticação, com o contexto completo — só assim os filtros de segurança de
 * verdade entram na jogada. Um {@code @WebMvcTest} não serviria aqui: testar segurança numa fatia
 * que mocka a própria segurança é teatro.
 */
class AuthHttpIT extends HttpIntegrationTest {

    @Test
    void semToken_retorna401ComOCorpoPadraoDaApi() throws Exception {
        mvc.perform(get("/api/boards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/boards"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void tokenAdulterado_retorna401() throws Exception {
        mvc.perform(get("/api/boards").header(HttpHeaders.AUTHORIZATION, "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registrarDepoisLogar_devolveTokensUsaveis() throws Exception {
        String email = emailUnico();

        String doRegistro = registrar(email, "senha-secreta");
        mvc.perform(get("/api/boards").with(comToken(JsonPath.read(doRegistro, "$.accessToken"))))
                .andExpect(status().isOk());

        String doLogin = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"email":"%s","password":"senha-secreta"}
                                 """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/boards").with(comToken(JsonPath.read(doLogin, "$.accessToken"))))
                .andExpect(status().isOk());
    }

    @Test
    void logarComSenhaErrada_retorna401() throws Exception {
        String email = emailUnico();
        registrar(email, "senha-secreta");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"email":"%s","password":"senha-errada"}
                                 """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registrarComEmailJaUsado_retorna409() throws Exception {
        String email = emailUnico();
        registrar(email, "senha-secreta");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"Outro","email":"%s","password":"outra-senha"}
                                 """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
