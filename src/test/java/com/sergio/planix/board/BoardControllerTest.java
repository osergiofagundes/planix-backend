package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP: status e corpo de erro. O service é mockado — aqui só importa a camada web.
 *
 * <p>Os filtros de segurança ficam desligados de propósito: esta fatia prova validação e formato
 * de resposta, não autenticação. Testar segurança numa fatia que mocka a própria segurança é
 * teatro — quem prova que a rota exige token é um teste de contexto completo.
 */
@WebMvcTest(BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean BoardService service;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void criarSemNome_retorna400ComOCampoNoFieldErrors() throws Exception {
        mvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
