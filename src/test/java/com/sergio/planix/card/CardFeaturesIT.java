package com.sergio.planix.card;

import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.checklist.ChecklistItemService;
import com.sergio.planix.checklist.dto.ChecklistItemRequest;
import com.sergio.planix.checklist.dto.ChecklistItemResponse;
import com.sergio.planix.comment.CommentService;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.label.LabelService;
import com.sergio.planix.label.dto.LabelRequest;
import com.sergio.planix.label.dto.LabelResponse;
import com.sergio.planix.link.CardLinkService;
import com.sergio.planix.link.dto.CardLinkRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CardFeaturesIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired LabelService labelService;
    @Autowired ChecklistItemService checklistService;
    @Autowired CommentService commentService;
    @Autowired CardLinkService linkService;

    @Test
    void etiquetasAnexamDesanexamESaoRemovidasDosCartoesAoExcluir() {
        BoardResponse board = boardService.create(quadroAberto("Etiquetas"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Comprar domínio"));

        LabelResponse urgente = labelService.create(board.id(), new LabelRequest("Urgente", "#e53935"));
        LabelResponse bug = labelService.create(board.id(), new LabelRequest("Bug", "#fdd835"));

        labelService.attach(card.id(), urgente.id());
        labelService.attach(card.id(), bug.id());
        assertThat(cardService.get(card.id()).labels())
                .extracting(LabelResponse::name)
                .containsExactly("Bug", "Urgente");

        labelService.detach(card.id(), bug.id());
        assertThat(cardService.get(card.id()).labels())
                .extracting(LabelResponse::name)
                .containsExactly("Urgente");

        labelService.delete(urgente.id());
        assertThat(cardService.get(card.id()).labels()).isEmpty();
        assertThat(cardService.get(card.id()).title()).isEqualTo("Comprar domínio");

        boardService.delete(board.id(), "Etiquetas");
    }

    @Test
    void checklistAlternaFeitoEMantemAOrdemAoRemoverDoMeio() {
        BoardResponse board = boardService.create(quadroAberto("Checklist"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Publicar site"));

        ChecklistItemResponse um = checklistService.create(card.id(), new ChecklistItemRequest("Escrever o texto"));
        ChecklistItemResponse dois = checklistService.create(card.id(), new ChecklistItemRequest("Revisar"));
        checklistService.create(card.id(), new ChecklistItemRequest("Publicar"));

        assertThat(checklistService.toggle(um.id()).done()).isTrue();
        assertThat(checklistService.toggle(um.id()).done()).isFalse();

        assertThat(checklistService.toggle(dois.id()).done()).isTrue();
        checklistService.delete(dois.id());

        assertThat(checklistService.listByCard(card.id()))
                .extracting(ChecklistItemResponse::text, ChecklistItemResponse::position)
                .containsExactly(tuple("Escrever o texto", 0), tuple("Publicar", 1));

        boardService.delete(board.id(), "Checklist");
    }

    @Test
    void comentarioELinkFicamLigadosAoCartao() {
        BoardResponse board = boardService.create(quadroAberto("Extras"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Estudar JPA"));

        commentService.create(card.id(), new CommentRequest("Começar pelo capítulo de mapeamentos", null));
        linkService.create(card.id(), new CardLinkRequest("https://spring.io/projects/spring-data-jpa", "Docs"));

        assertThat(commentService.listByCard(card.id())).hasSize(1);
        assertThat(linkService.listByCard(card.id())).hasSize(1);

        boardService.delete(board.id(), "Extras");
    }
}
