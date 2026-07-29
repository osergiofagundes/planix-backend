package com.sergio.planix.card;

import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.card.dto.CardUpdateRequest;
import com.sergio.planix.history.dto.CardChangeResponse;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CardFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;

    @Test
    void moverCartaoEntreListas_renumeraAsDuasERegistraNoHistorico() {
        BoardResponse board = boardService.create(new BoardRequest("Cartões", null));
        BoardListResponse aFazer = listService.create(board.id(), new BoardListRequest("A Fazer"));
        BoardListResponse fazendo = listService.create(board.id(), new BoardListRequest("Fazendo"));

        CardResponse c1 = cardService.create(aFazer.id(), new CardCreateRequest("Comprar domínio"));
        CardResponse c2 = cardService.create(aFazer.id(), new CardCreateRequest("Configurar DNS"));
        CardResponse c3 = cardService.create(aFazer.id(), new CardCreateRequest("Publicar site"));
        assertThat(List.of(c1.position(), c2.position(), c3.position())).containsExactly(0, 1, 2);

        cardService.move(c1.id(), fazendo.id(), 0);

        assertThat(cardService.listByList(aFazer.id()))
                .extracting(CardResponse::title, CardResponse::position)
                .containsExactly(tuple("Configurar DNS", 0), tuple("Publicar site", 1));
        assertThat(cardService.listByList(fazendo.id()))
                .extracting(CardResponse::title, CardResponse::position)
                .containsExactly(tuple("Comprar domínio", 0));
        assertThat(cardService.listChanges(c1.id()))
                .extracting(CardChangeResponse::field)
                .containsExactly("list_id");

        boardService.delete(board.id(), "Cartões");
    }

    @Test
    void moverDentroDaMesmaLista_naoDuplicaPositions() {
        BoardResponse board = boardService.create(new BoardRequest("Mesma lista", null));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));

        CardResponse c1 = cardService.create(lista.id(), new CardCreateRequest("Um"));
        cardService.create(lista.id(), new CardCreateRequest("Dois"));
        CardResponse c3 = cardService.create(lista.id(), new CardCreateRequest("Três"));

        cardService.move(c3.id(), lista.id(), 0);

        assertThat(cardService.listByList(lista.id()))
                .extracting(CardResponse::title, CardResponse::position)
                .containsExactly(tuple("Três", 0), tuple("Um", 1), tuple("Dois", 2));
        // mover dentro da mesma lista não é mudança de lista: nada no histórico
        assertThat(cardService.listChanges(c1.id())).isEmpty();

        boardService.delete(board.id(), "Mesma lista");
    }

    @Test
    void historicoRegistraSoOQueMudou_eConclusaoGravaCompletedAt() {
        BoardResponse board = boardService.create(new BoardRequest("Histórico", null));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Comprar domínio"));

        cardService.update(card.id(), new CardUpdateRequest("Comprar o domínio", null, null, Priority.HIGH));
        assertThat(cardService.listChanges(card.id()))
                .extracting(CardChangeResponse::field)
                .containsExactlyInAnyOrder("title", "priority");

        // reenviar os mesmos valores não cria nada novo
        cardService.update(card.id(), new CardUpdateRequest("Comprar o domínio", null, null, Priority.HIGH));
        assertThat(cardService.listChanges(card.id())).hasSize(2);

        assertThat(cardService.setCompleted(card.id(), true).completedAt()).isNotNull();
        assertThat(cardService.setCompleted(card.id(), false).completedAt()).isNull();
        assertThat(cardService.listChanges(card.id()))
                .extracting(CardChangeResponse::field)
                .contains("completed");

        boardService.delete(board.id(), "Histórico");
    }

    @Test
    void excluirCartaoDoMeio_mantemAOrdemContigua() {
        BoardResponse board = boardService.create(new BoardRequest("Exclusão", null));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));

        cardService.create(lista.id(), new CardCreateRequest("Um"));
        CardResponse meio = cardService.create(lista.id(), new CardCreateRequest("Dois"));
        cardService.create(lista.id(), new CardCreateRequest("Três"));

        cardService.delete(meio.id());

        assertThat(cardService.listByList(lista.id()))
                .extracting(CardResponse::title, CardResponse::position)
                .containsExactly(tuple("Um", 0), tuple("Três", 1));

        boardService.delete(board.id(), "Exclusão");
    }
}
