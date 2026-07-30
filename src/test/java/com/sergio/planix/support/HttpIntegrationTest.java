package com.sergio.planix.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes que exercitam a API por HTTP com o contexto completo: aqui a cadeia de filtros de
 * segurança é a de produção, então dá para provar que uma rota exige token de verdade.
 *
 * <p>De propósito <b>não</b> estende {@link AuthenticatedIntegrationTest}: quem quiser um usuário
 * autenticado pede um token com {@link #tokenDeUsuarioNovo()} e o manda no cabeçalho, como um
 * cliente real faria. Colocar alguém no {@code SecurityContextHolder} aqui só mascararia o que
 * estes testes querem verificar.
 */
@AutoConfigureMockMvc
public abstract class HttpIntegrationTest extends IntegrationTest {

    @Autowired protected MockMvc mvc;

    /**
     * O JUnit reaproveita threads entre classes e o SecurityContextHolder é uma ThreadLocal: sem
     * esta limpeza, um teste que deveria bater em 401 herdaria o usuário de outra classe e passaria
     * por engano.
     */
    @BeforeEach
    void semUsuarioNoContexto() {
        SecurityContextHolder.clearContext();
    }

    /** Registra um usuário pela própria API e devolve o access token dele. */
    protected String tokenDeUsuarioNovo() throws Exception {
        return JsonPath.read(registrar(emailUnico(), "senha-de-teste"), "$.accessToken");
    }

    /** Corpo da resposta de {@code POST /api/auth/register}, já validado como 201. */
    protected String registrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"name":"Teste","email":"%s","password":"%s"}
                                 """.formatted(email, senha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Os testes não fazem rollback e uq_users_email é real: cada usuário precisa do seu e-mail. */
    protected static String emailUnico() {
        return "user-" + UUID.randomUUID() + "@planix.test";
    }

    /**
     * Assina a requisição com o token, do jeito que um cliente faria:
     * {@code mvc.perform(get("/api/boards").with(comToken(token)))}.
     */
    protected static RequestPostProcessor comToken(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
