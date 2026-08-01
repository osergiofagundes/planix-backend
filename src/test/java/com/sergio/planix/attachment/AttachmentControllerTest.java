package com.sergio.planix.attachment;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.JwtService;
import com.sergio.planix.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttachmentControllerTest {

    private static final long LIMITE_EM_BYTES = 10L * 1024 * 1024;

    @Autowired MockMvc mvc;

    @MockitoBean AttachmentService service;
    @MockitoBean FileStorageService storage;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean JwtService jwtService;

    @Test
    void uploadAcimaDoLimite_retorna413ComCorpoApiError() throws Exception {
        when(service.upload(eq(1L), any(MultipartFile.class)))
                .thenThrow(new MaxUploadSizeExceededException(LIMITE_EM_BYTES));

        MockMultipartFile arquivo = new MockMultipartFile("file", "grande.bin",
                "application/octet-stream", "conteúdo".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/cards/1/attachments").file(arquivo))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.path").value("/api/cards/1/attachments"))
                .andExpect(jsonPath("$.message").value("Arquivo maior que o limite permitido"));
    }

    @Test
    void baixarAnexoInexistente_retorna404ComCorpoApiError() throws Exception {
        when(service.getEntity(999L)).thenThrow(new NotFoundException("Anexo 999 não encontrado"));

        mvc.perform(get("/api/attachments/999/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Anexo 999 não encontrado"));
    }
}
