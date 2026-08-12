package com.sergio.planix.board;

import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BoardFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;

    @Test
    void listasEntramNoFimEMovemComRenumeracao() {
        BoardResponse board = boardService.create(quadroAberto("Ordenação"));

        BoardListResponse aFazer = listService.create(board.id(), new BoardListRequest("A Fazer"));
        BoardListResponse fazendo = listService.create(board.id(), new BoardListRequest("Fazendo"));
        BoardListResponse concluido = listService.create(board.id(), new BoardListRequest("Concluído"));
        assertThat(List.of(aFazer.position(), fazendo.position(), concluido.position()))
                .containsExactly(0, 1, 2);

        listService.move(concluido.id(), 0);

        assertThat(listService.listByBoard(board.id()))
                .extracting(BoardListResponse::name, BoardListResponse::position)
                .containsExactly(
                        tuple("Concluído", 0),
                        tuple("A Fazer", 1),
                        tuple("Fazendo", 2));

        boardService.delete(board.id(), "Ordenação");
    }

    @Test
    void excluirQuadroComConteudo_exigeNomeEDepoisCascateia() {
        BoardResponse board = boardService.create(quadroAberto("Projeto", "Quadro do teste"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));

        assertThatThrownBy(() -> boardService.delete(board.id(), null))
                .isInstanceOf(BoardNotEmptyException.class);

        boardService.delete(board.id(), "Projeto");

        assertThat(boardService.list(null)).extracting(BoardResponse::id).doesNotContain(board.id());
        assertThatThrownBy(() -> listService.get(lista.id()))
                .isInstanceOf(NotFoundException.class);
    }
}
