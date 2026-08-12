package com.sergio.planix.attachment;

import com.sergio.planix.attachment.dto.AttachmentResponse;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired AttachmentService attachmentService;
    @Autowired Path uploadDir;

    @Test
    void uploadGravaNoDisco_eDeleteApagaArquivoEMetadados() throws Exception {
        BoardResponse board = boardService.create(quadroAberto("Anexos"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Enviar proposta"));

        MockMultipartFile file = new MockMultipartFile("file", "proposta.txt",
                "text/plain", "conteúdo da proposta".getBytes(StandardCharsets.UTF_8));

        AttachmentResponse anexo = attachmentService.upload(card.id(), file);

        assertThat(anexo.originalFilename()).isEqualTo("proposta.txt");
        assertThat(anexo.storedFilename()).endsWith(".txt").isNotEqualTo("proposta.txt");
        assertThat(anexo.contentType()).isEqualTo("text/plain");

        Path noDisco = uploadDir.resolve(anexo.storedFilename());
        assertThat(Files.exists(noDisco)).isTrue();
        assertThat(Files.readString(noDisco, StandardCharsets.UTF_8)).isEqualTo("conteúdo da proposta");
        assertThat(attachmentService.list(card.id())).hasSize(1);

        attachmentService.delete(anexo.id());

        assertThat(Files.exists(noDisco)).isFalse();
        assertThat(attachmentService.list(card.id())).isEmpty();

        boardService.delete(board.id(), "Anexos");
    }
}
