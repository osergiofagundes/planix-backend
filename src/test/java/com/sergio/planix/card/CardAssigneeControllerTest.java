package com.sergio.planix.card;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.common.NotBoardMemberException;
import com.sergio.planix.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardAssigneeController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardAssigneeControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CardAssigneeService service;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void atribuirEDesatribuir_respondem204SemCorpo() throws Exception {
        doNothing().when(service).assign(1L, 2L);
        doNothing().when(service).unassign(1L, 2L);

        mvc.perform(post("/api/cards/1/assignees/2"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/cards/1/assignees/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void atribuirQuemNaoEMembro_retorna409ComAMensagemDoService() throws Exception {
        doThrow(new NotBoardMemberException("O usuário 7 não é membro deste quadro"))
                .when(service).assign(1L, 7L);

        mvc.perform(post("/api/cards/1/assignees/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/cards/1/assignees/7"))
                .andExpect(jsonPath("$.message").value("O usuário 7 não é membro deste quadro"));
    }

    @Test
    void cartaoQueVoceNaoEnxerga_retorna404ComCorpoApiError() throws Exception {
        doThrow(new NotFoundException("Cartão 99 não encontrado")).when(service).assign(99L, 2L);

        mvc.perform(post("/api/cards/99/assignees/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cartão 99 não encontrado"));
    }
}
