package com.sergio.planix.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public abstract class HttpIntegrationTest extends IntegrationTest {

    @Autowired protected MockMvc mvc;

    @BeforeEach
    void semUsuarioNoContexto() {
        SecurityContextHolder.clearContext();
    }

    protected String tokenDeUsuarioNovo() throws Exception {
        return JsonPath.read(registrar(emailUnico(), "senha-de-teste"), "$.accessToken");
    }

    protected String registrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"Teste","email":"%s","password":"%s"}
                                 """.formatted(email, senha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected static String emailUnico() {
        return "user-" + UUID.randomUUID() + "@planix.test";
    }

    protected int equipePadrao(String token) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/teams").with(comToken(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$[0].id");
    }

    protected int criarQuadro(String token, String nome) throws Exception {
        return criarQuadro(token, nome, "TEAM");
    }

    protected int criarQuadro(String token, String nome, String visibilidade) throws Exception {
        return idDe(mvc.perform(post("/api/boards").with(comToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"teamId":%d,"name":"%s","visibility":"%s"}
                                 """.formatted(equipePadrao(token), nome, visibilidade)))
                .andExpect(status().isCreated()));
    }

    protected static int idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    protected static RequestPostProcessor comToken(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
