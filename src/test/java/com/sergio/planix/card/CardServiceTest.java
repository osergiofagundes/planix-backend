package com.sergio.planix.card;

import com.sergio.planix.board.Board;
import com.sergio.planix.card.dto.CardUpdateRequest;
import com.sergio.planix.history.CardChange;
import com.sergio.planix.history.CardChangeRepository;
import com.sergio.planix.list.BoardList;
import com.sergio.planix.list.BoardListRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** O histórico diff e o completed_at — as duas regras mais fáceis de quebrar sem perceber. */
class CardServiceTest {

    private final CardRepository cardRepo = mock(CardRepository.class);
    private final BoardListRepository listRepo = mock(BoardListRepository.class);
    private final CardChangeRepository changeRepo = mock(CardChangeRepository.class);
    private final CardService service = new CardService(cardRepo, listRepo, changeRepo);

    @Test
    void atualizarComOsMesmosValores_naoGeraHistorico() {
        Card card = card("Comprar domínio");
        when(cardRepo.findById(1L)).thenReturn(Optional.of(card));

        service.update(1L, new CardUpdateRequest("Comprar domínio", null, null, Priority.NONE));

        verify(changeRepo).saveAll(List.<CardChange>of());
    }

    @Test
    void atualizarTituloEPrioridade_geraDuasEntradasNoHistorico() {
        Card card = card("Comprar domínio");
        when(cardRepo.findById(1L)).thenReturn(Optional.of(card));

        service.update(1L, new CardUpdateRequest("Comprar o domínio", null, null, Priority.HIGH));

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
        when(cardRepo.findById(1L)).thenReturn(Optional.of(card));

        service.update(1L, new CardUpdateRequest("Comprar domínio", null, null, null));

        assertThat(card.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    void concluirPreencheCompletedAt_eReabrirLimpa() {
        Card card = card("Comprar domínio");
        when(cardRepo.findById(1L)).thenReturn(Optional.of(card));

        service.setCompleted(1L, true);
        assertThat(card.isCompleted()).isTrue();
        assertThat(card.getCompletedAt()).isNotNull();

        service.setCompleted(1L, false);
        assertThat(card.isCompleted()).isFalse();
        assertThat(card.getCompletedAt()).isNull();

        verify(changeRepo, times(2)).save(any(CardChange.class));
    }

    @Test
    void concluirCartaoJaConcluido_naoGeraHistorico() {
        Card card = card("Comprar domínio");
        card.setCompleted(true);
        when(cardRepo.findById(1L)).thenReturn(Optional.of(card));

        service.setCompleted(1L, true);

        verify(changeRepo, never()).save(any(CardChange.class));
    }

    private static Card card(String title) {
        Board board = new Board("Quadro", null);
        board.setId(1L);
        BoardList list = new BoardList(board, "A Fazer", 0);
        list.setId(10L);
        Card card = new Card(list, title, 0);
        card.setId(100L);
        return card;
    }
}
