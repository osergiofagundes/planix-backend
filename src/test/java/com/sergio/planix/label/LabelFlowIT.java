package com.sergio.planix.label;

import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.LabelNameAlreadyUsedException;
import com.sergio.planix.label.dto.LabelRequest;
import com.sergio.planix.label.dto.LabelResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabelFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired LabelService labelService;

    @Test
    void duasEtiquetasComOMesmoNomeNoMesmoQuadro_naoPodem() {
        BoardResponse quadro = boardService.create(quadroAberto("Quadro"));
        labelService.create(quadro.id(), new LabelRequest("Urgente", "vermelho"));

        assertThatThrownBy(() -> labelService.create(quadro.id(), new LabelRequest("Urgente", "azul")))
                .isInstanceOf(LabelNameAlreadyUsedException.class)
                .hasMessageContaining("Urgente");

        assertThat(labelService.listByBoard(quadro.id()))
                .extracting(LabelResponse::color)
                .containsExactly("vermelho");
    }

    @Test
    void mesmoNomeEmQuadrosDiferentes_saoPermitidas() {
        BoardResponse trabalho = boardService.create(quadroAberto("Trabalho"));
        BoardResponse pessoal = boardService.create(quadroAberto("Pessoal"));

        labelService.create(trabalho.id(), new LabelRequest("Urgente", "vermelho"));
        labelService.create(pessoal.id(), new LabelRequest("Urgente", "vermelho"));

        assertThat(labelService.listByBoard(trabalho.id())).hasSize(1);
        assertThat(labelService.listByBoard(pessoal.id())).hasSize(1);
    }
}
