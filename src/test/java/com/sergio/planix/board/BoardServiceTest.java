package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.list.BoardListRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regra de negócio da exclusão de quadro, isolada do banco com mocks. */
class BoardServiceTest {

    private final BoardRepository repo = mock(BoardRepository.class);
    private final BoardListRepository listRepo = mock(BoardListRepository.class);
    // Os métodos de acesso são void: o mock não faz nada, ou seja, autoriza. É o que estes
    // testes querem — aqui a regra sob teste é a confirmação pelo nome, não a autorização.
    private final BoardAccess access = mock(BoardAccess.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final BoardService service = new BoardService(repo, listRepo, access, currentUser);

    private static final User DONO = new User("Dono", "dono@planix.test", "hash");

    @Test
    void excluirQuadroComConteudoSemConfirmacao_lancaExcecao() {
        Board board = new Board(DONO, "Estudos", null);
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, null))
                .isInstanceOf(BoardNotEmptyException.class);

        verify(repo, never()).delete(any());   // não pode ter apagado
    }

    @Test
    void excluirQuadroComConteudoComNomeErrado_lancaExcecao() {
        Board board = new Board(DONO, "Estudos", null);
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, "estudos"))
                .isInstanceOf(BoardNotEmptyException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void excluirQuadroComConteudoComNomeCorreto_apaga() {
        Board board = new Board(DONO, "Estudos", null);
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        service.delete(1L, "Estudos");

        verify(repo).delete(board);
    }

    @Test
    void excluirQuadroVazio_naoExigeConfirmacao() {
        Board board = new Board(DONO, "Estudos", null);
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(false);

        service.delete(1L, null);

        verify(repo).delete(board);
    }

    @Test
    void excluirQuadro_exigeSerODono() {
        doThrow(new com.sergio.planix.common.NotFoundException("Quadro 1 não encontrado"))
                .when(access).requireOwner(1L);

        assertThatThrownBy(() -> service.delete(1L, "Estudos"))
                .isInstanceOf(com.sergio.planix.common.NotFoundException.class);

        verify(repo, never()).delete(any());
    }
}
