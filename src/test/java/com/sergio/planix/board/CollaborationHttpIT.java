package com.sergio.planix.board;

import com.jayway.jsonpath.JsonPath;
import com.sergio.planix.support.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollaborationHttpIT extends HttpIntegrationTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Test
    void doConviteDeEquipeAoQuadroCompartilhado_comAsPortasCertasFechadas() throws Exception {
        String a = tokenDeUsuarioNovo();
        String b = tokenDeUsuarioNovo();
        String c = tokenDeUsuarioNovo();

        int equipe = equipePadrao(a);
        int quadro = criarQuadro(a, "Compartilhado");

        String convite = JsonPath.read(
                mvc.perform(post("/api/teams/{id}/invites", equipe).with(comToken(a))
                                .contentType(JSON).content("{\"maxUses\":1}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.token").isNotEmpty())
                        .andExpect(jsonPath("$.maxUses").value(1))
                        .andReturn().getResponse().getContentAsString(),
                "$.token");

        mvc.perform(get("/api/teams/{id}/invites", equipe).with(comToken(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uses").value(0))
                .andExpect(jsonPath("$[0].maxUses").value(1))
                .andExpect(jsonPath("$[0].role").value("MEMBER"))
                .andExpect(jsonPath("$[0].token").doesNotExist());

        mvc.perform(get("/api/boards/{id}", quadro).with(comToken(b)))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/invites/preview").with(comToken(b)).contentType(JSON)
                        .content(corpoDoToken(convite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").isNotEmpty())
                .andExpect(jsonPath("$.invitedBy.id").exists());

        mvc.perform(post("/api/invites/accept").with(comToken(b)).contentType(JSON)
                        .content(corpoDoToken(convite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equipe))
                .andExpect(jsonPath("$.myRole").value("MEMBER"));

        mvc.perform(get("/api/boards").with(comToken(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(quadro)));

        int lista = idDe(mvc.perform(post("/api/boards/{id}/lists", quadro).with(comToken(b))
                        .contentType(JSON).content("{\"name\":\"A Fazer\"}"))
                .andExpect(status().isCreated()));
        int cartao = idDe(mvc.perform(post("/api/lists/{id}/cards", lista).with(comToken(b))
                        .contentType(JSON).content("{\"title\":\"Cartão do B\"}"))
                .andExpect(status().isCreated()));
        mvc.perform(delete("/api/cards/{id}", cartao).with(comToken(b)))
                .andExpect(status().isNoContent());

        mvc.perform(put("/api/boards/{id}", quadro).with(comToken(b))
                        .contentType(JSON).content("{\"name\":\"Meu agora\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/boards/" + quadro))
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(delete("/api/boards/{id}", quadro).with(comToken(b)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/teams/{id}/invites", equipe).with(comToken(b))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/invites/accept").with(comToken(c)).contentType(JSON)
                        .content(corpoDoToken(convite)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Convite inválido ou expirado"));

        mvc.perform(get("/api/boards/{id}/members", quadro).with(comToken(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/api/boards/{id}/members/me", quadro).with(comToken(b)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/teams/{id}/members/me", equipe).with(comToken(b)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/boards").with(comToken(b)))
                .andExpect(jsonPath("$[*].id", not(hasItem(quadro))));
        mvc.perform(delete("/api/teams/{id}/members/me", equipe).with(comToken(a)))
                .andExpect(status().isForbidden());
    }

    @Test
    void quadroFechado_soAbreParaQuemODonoAdiciona() throws Exception {
        String a = tokenDeUsuarioNovo();
        String b = tokenDeUsuarioNovo();

        int equipe = equipePadrao(a);
        int quadroAberto = criarQuadro(a, "Comercial");
        int quadroFechado = criarQuadro(a, "Diretoria", "RESTRICTED");

        String convite = JsonPath.read(
                mvc.perform(post("/api/teams/{id}/invites", equipe).with(comToken(a))
                                .contentType(JSON).content("{\"maxUses\":1}"))
                        .andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/invites/accept").with(comToken(b)).contentType(JSON)
                        .content(corpoDoToken(convite)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/boards").with(comToken(b)))
                .andExpect(jsonPath("$[*].id", hasItem(quadroAberto)))
                .andExpect(jsonPath("$[*].id", not(hasItem(quadroFechado))));

        int idDoB = JsonPath.read(mvc.perform(get("/api/auth/me").with(comToken(b)))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/boards/{id}/members/candidates", quadroFechado).with(comToken(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(idDoB)));

        mvc.perform(post("/api/boards/{id}/members", quadroFechado).with(comToken(a))
                        .contentType(JSON).content("{\"userId\":%d}".formatted(idDoB)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/boards").with(comToken(b)))
                .andExpect(jsonPath("$[*].id", hasItem(quadroFechado)));

        mvc.perform(delete("/api/boards/{id}/members/{userId}", quadroFechado, idDoB).with(comToken(a)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/boards/{id}", quadroFechado).with(comToken(b)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rotasDeConviteSemToken_retornam401() throws Exception {
        mvc.perform(post("/api/invites/accept").contentType(JSON).content(corpoDoToken("qualquer")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/invites/preview").contentType(JSON).content(corpoDoToken("qualquer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aceitarComTokenVazioNoCorpo_retorna400() throws Exception {
        mvc.perform(post("/api/invites/accept").with(comToken(tokenDeUsuarioNovo()))
                        .contentType(JSON).content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.token").exists());
    }

    private static String corpoDoToken(String token) {
        return "{\"token\":\"%s\"}".formatted(token);
    }
}
