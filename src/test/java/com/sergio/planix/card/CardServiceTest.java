package com.sergio.planix.card;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.dto.CardUpdateRequest;
import com.sergio.planix.common.CrossBoardMoveException;
import com.sergio.planix.history.CardChange;
import com.sergio.planix.history.CardChangeRepository;
import com.sergio.planix.list.BoardList;
import com.sergio.planix.list.BoardListRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardServiceTest {

    private final CardRepository cardRepo = mock(CardRepository.class);
    private final BoardListRepository listRepo = mock(BoardListRepository.class);
    private final CardChangeRepository changeRepo = mock(CardChangeRepository.class);
    private final CardAccess cardAccess = mock(CardAccess.class);
    private final BoardAccess boardAccess = mock(BoardAccess.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final CardService service = new CardService(
            cardRepo, listRepo, changeRepo, cardAccess, boardAccess, currentUser);

    @Test
    void atualizarComOsMesmosValores_naoGeraHistorico() {
        Card card = card("Comprar domínio");
        when(cardAccess.require(100L)).thenReturn(card);

        service.update(100L, new CardUpdateRequest("Comprar domínio", null, null, Priority.NONE));

        verify(changeRepo).saveAll(List.<CardChange>of());
    }

    @Test
    void atualizarTituloEPrioridade_geraDuasEntradasNoHistorico() {
        Card card = card("Comprar domínio");
        when(cardAccess.require(100L)).thenReturn(card);

        service.update(100L, new CardUpdateRequest("Comprar o domínio", null, null, Priority.HIGH));

        ArgumentCaptor<List<CardChange>> captor = ArgumentCaptor.captor();
        verify(changeRepo).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(CardChange::getField)
                .containsExactlyInAnyOrder("title", "priority");
        assertThat(card.getTitle()).isEqualTo("Comprar o domínio");
        assertThat(card.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void prioridadeNulaNoRequest_viraNONE_semQuebrar() {
        Card card = card("Comprar domínio");
        card.setPriority(Priority.HIGH);
        when(cardAccess.require(100L)).thenReturn(card);

        service.update(100L, new CardUpdateRequest("Comprar domínio", null, null, null));

        assertThat(card.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    void concluirPreencheCompletedAt_eReabrirLimpa() {
        Card card = card("Comprar domínio");
        when(cardAccess.require(100L)).thenReturn(card);

        service.setCompleted(100L, true);
        assertThat(card.isCompleted()).isTrue();
        assertThat(card.getCompletedAt()).isNotNull();

        service.setCompleted(100L, false);
        assertThat(card.isCompleted()).isFalse();
        assertThat(card.getCompletedAt()).isNull();

        verify(changeRepo, times(2)).save(any(CardChange.class));
    }

    @Test
    void concluirCartaoJaConcluido_naoGeraHistorico() {
        Card card = card("Comprar domínio");
        card.setCompleted(true);
        when(cardAccess.require(100L)).thenReturn(card);

        service.setCompleted(100L, true);

        verify(changeRepo, never()).save(any(CardChange.class));
    }

    @Test
    void moverParaListaDeOutroQuadro_recusaCom409() {
        Card card = card("Comprar domínio");
        when(cardAccess.require(100L)).thenReturn(card);

        Board outro = board(2L);
        BoardList listaDoOutroQuadro = new BoardList(outro, "A Fazer", 0);
        listaDoOutroQuadro.setId(20L);
        when(listRepo.findById(20L)).thenReturn(Optional.of(listaDoOutroQuadro));
        when(boardAccess.isMember(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.move(100L, 20L, 0))
                .isInstanceOf(CrossBoardMoveException.class);

        verify(changeRepo, never()).save(any(CardChange.class));
        assertThat(card.getList().getId()).isEqualTo(10L);
    }

    private static Board board(Long id) {
        Board board = new Board(new User("Dono", "dono@planix.test", "hash"), "Quadro", null, null);
        board.setId(id);
        return board;
    }

    private static Card card(String title) {
        BoardList list = new BoardList(board(1L), "A Fazer", 0);
        list.setId(10L);
        Card card = new Card(list, title, 0);
        card.setId(100L);
        return card;
    }
}
