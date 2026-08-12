package com.sergio.planix.label;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.common.exception.LabelNameAlreadyUsedException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.label.dto.LabelRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
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

@WebMvcTest(LabelController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabelControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean LabelService service;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void criarSemNomeNemCor_retorna400ComOsDoisCamposNoFieldErrors() throws Exception {
        mvc.perform(post("/api/boards/1/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"color\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.color").exists());
    }

    @Test
    void quadroInexistente_retorna404ComCorpoApiError() throws Exception {
        when(service.listByBoard(999L))
                .thenThrow(new NotFoundException("Quadro 999 não encontrado"));

        mvc.perform(get("/api/boards/999/labels"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/boards/999/labels"))
                .andExpect(jsonPath("$.message").value("Quadro 999 não encontrado"));
    }

    @Test
    void nomeDuplicadoNoQuadro_retorna409ComAMensagemDoService() throws Exception {
        when(service.create(eq(1L), any(LabelRequest.class)))
                .thenThrow(new LabelNameAlreadyUsedException(
                        "O quadro já tem uma etiqueta chamada \"Urgente\""));

        mvc.perform(post("/api/boards/1/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Urgente\",\"color\":\"azul\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "O quadro já tem uma etiqueta chamada \"Urgente\""));
    }

    @Test
    void violacaoDeUniqueNoBanco_retorna409EmVezDe500() throws Exception {
        when(service.create(eq(1L), any(LabelRequest.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uq_label_board_name\""));

        mvc.perform(post("/api/boards/1/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Urgente\",\"color\":\"azul\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "A operação viola uma restrição de integridade dos dados"));
    }
}
