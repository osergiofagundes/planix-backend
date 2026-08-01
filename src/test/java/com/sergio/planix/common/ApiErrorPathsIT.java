package com.sergio.planix.common;

import com.jayway.jsonpath.JsonPath;
import com.sergio.planix.support.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorPathsIT extends HttpIntegrationTest {

    @Test
    void cartaoInexistente_retorna404ComCorpoApiError() throws Exception {
        RequestPostProcessor token = comToken(tokenDeUsuarioNovo());

        mvc.perform(get("/api/cards/999999").with(token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/cards/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void excluirQuadroComConteudoSemConfirmar_retorna409() throws Exception {
        RequestPostProcessor token = comToken(tokenDeUsuarioNovo());
        long quadroId = criarQuadroComUmaLista(token, "Projeto");

        mvc.perform(delete("/api/boards/" + quadroId).with(token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void excluirQuadroComConteudoConfirmandoONome_retorna204() throws Exception {
        RequestPostProcessor token = comToken(tokenDeUsuarioNovo());
        long quadroId = criarQuadroComUmaLista(token, "Projeto");

        mvc.perform(delete("/api/boards/" + quadroId)
                        .param("confirmationName", "Projeto")
                        .with(token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/boards/" + quadroId).with(token))
                .andExpect(status().isNotFound());
    }

    private long criarQuadroComUmaLista(RequestPostProcessor token, String nome) throws Exception {
        String quadro = mvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"%s"}
                                 """.formatted(nome))
                        .with(token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long quadroId = ((Number) JsonPath.read(quadro, "$.id")).longValue();

        mvc.perform(post("/api/boards/" + quadroId + "/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A Fazer\"}")
                        .with(token))
                .andExpect(status().isCreated());

        return quadroId;
    }
}
