package com.sergio.planix.comment;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.comment.dto.CommentReactionRequest;
import com.sergio.planix.comment.dto.CommentReactionSummary;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.common.exception.CommentDeletedException;
import com.sergio.planix.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void reagirSemEmoji_retorna400ComOCampoNoFieldErrors() throws Exception {
        mvc.perform(post("/api/comments/1/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.emoji").exists());
    }

    @Test
    void reagir_retornaAsReacoesAgrupadasPorEmoji() throws Exception {
        when(service.toggleReaction(eq(300L), any(CommentReactionRequest.class)))
                .thenReturn(List.of(new CommentReactionSummary("👍", 2, true,
                        List.of(new UserSummary(1L, "Sérgio", null),
                                new UserSummary(2L, "Ana", null)))));

        mvc.perform(post("/api/comments/300/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"👍\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emoji").value("👍"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].reactedByMe").value(true))
                .andExpect(jsonPath("$[0].users[1].name").value("Ana"));
    }

    @Test
    void responderComentarioExcluido_retorna409() throws Exception {
        when(service.create(eq(1L), any(CommentRequest.class)))
                .thenThrow(new CommentDeletedException("Comentário 300 foi excluído"));

        mvc.perform(post("/api/cards/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Ainda dá?\",\"parentId\":300}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Comentário 300 foi excluído"));
    }

    @Test
    void reagirEmComentarioInexistente_retorna404() throws Exception {
        when(service.toggleReaction(eq(999L), any(CommentReactionRequest.class)))
                .thenThrow(new NotFoundException("Comentário 999 não encontrado"));

        mvc.perform(post("/api/comments/999/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"🚀\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/comments/999/reactions"));
    }
}
