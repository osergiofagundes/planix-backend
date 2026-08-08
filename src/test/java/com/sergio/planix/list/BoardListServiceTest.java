package com.sergio.planix.list;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.board.BoardRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardListServiceTest {

    private final BoardListRepository repo = mock(BoardListRepository.class);
    private final BoardRepository boardRepo = mock(BoardRepository.class);
    private final BoardAccess access = mock(BoardAccess.class);
    private final BoardListService service = new BoardListService(repo, boardRepo, access);

    private final Board board = board();
    private final BoardList aFazer = list("A Fazer", 0, 10L);
    private final BoardList fazendo = list("Fazendo", 1, 11L);
    private final BoardList concluido = list("Concluído", 2, 12L);

    @Test
    void moverUltimaParaOInicio_renumeraTodasSemRepetir() {
        stubSiblings();
        when(repo.findById(12L)).thenReturn(Optional.of(concluido));

        service.move(12L, 0);

        assertThat(concluido.getPosition()).isZero();
        assertThat(aFazer.getPosition()).isEqualTo(1);
        assertThat(fazendo.getPosition()).isEqualTo(2);
    }

    @Test
    void moverParaPosicaoNegativa_fazClampNoZero() {
        stubSiblings();
        when(repo.findById(11L)).thenReturn(Optional.of(fazendo));

        service.move(11L, -5);

        assertThat(fazendo.getPosition()).isZero();
        assertThat(aFazer.getPosition()).isEqualTo(1);
        assertThat(concluido.getPosition()).isEqualTo(2);
    }

    @Test
    void moverParaPosicaoAlemDoFim_fazClampNoUltimoIndice() {
        stubSiblings();
        when(repo.findById(10L)).thenReturn(Optional.of(aFazer));

        service.move(10L, 99);

        assertThat(fazendo.getPosition()).isZero();
        assertThat(concluido.getPosition()).isEqualTo(1);
        assertThat(aFazer.getPosition()).isEqualTo(2);
    }

    private void stubSiblings() {
        when(access.isMember(1L)).thenReturn(true);
        when(repo.findByBoardIdOrderByPositionAsc(1L))
                .thenReturn(new ArrayList<>(List.of(aFazer, fazendo, concluido)));
    }

    private static Board board() {
        Board board = new Board(new User("Dono", "dono@planix.test", "hash"), "Quadro", null, null);
        board.setId(1L);
        return board;
    }

    private BoardList list(String name, int position, Long id) {
        BoardList list = new BoardList(board, name, position);
        list.setId(id);
        return list;
    }
}
