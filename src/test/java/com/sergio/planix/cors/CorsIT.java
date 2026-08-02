package com.sergio.planix.cors;

import com.sergio.planix.support.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsIT extends HttpIntegrationTest {

    private static final String ORIGEM_DO_VITE = "http://localhost:5173";

    @Test
    void preflightDaOrigemDoVite_retorna200SemPassarPelaAutenticacao() throws Exception {
        mvc.perform(options("/api/boards")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_VITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_VITE));
    }

    @Test
    void requisicaoComToken_devolveAllowOriginEExpoeOLocation() throws Exception {
        String token = tokenDeUsuarioNovo();

        mvc.perform(post("/api/boards").with(comToken(token))
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_VITE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Quadro do front\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_VITE))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                                           containsString(HttpHeaders.LOCATION)));
    }

    @Test
    void origemDesconhecida_naoRecebeAllowOrigin() throws Exception {
        mvc.perform(options("/api/boards")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
