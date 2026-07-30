package com.sergio.planix.comment;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP dos comentários: validação e 404, os dois casos que faltavam nos recursos pequenos. */
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CommentService service;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void criarSemTexto_retorna400ComOCampoNoFieldErrors() throws Exception {
        mvc.perform(post("/api/cards/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.text").exists());
    }

    @Test
    void listarComentariosDeCartaoInexistente_retorna404ComCorpoApiError() throws Exception {
        when(service.listByCard(999L))
                .thenThrow(new NotFoundException("Cartão 999 não encontrado"));

        mvc.perform(get("/api/cards/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/cards/999/comments"))
                .andExpect(jsonPath("$.message").value("Cartão 999 não encontrado"));
    }

    @Test
    void comentarEmCartaoInexistente_retorna404() throws Exception {
        when(service.create(eq(999L), any(CommentRequest.class)))
                .thenThrow(new NotFoundException("Cartão 999 não encontrado"));

        mvc.perform(post("/api/cards/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Comentário órfão\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
