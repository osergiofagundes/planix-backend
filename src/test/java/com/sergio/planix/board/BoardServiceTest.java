package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListRepository;
import com.sergio.planix.member.BoardMemberRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regra de negócio da exclusão de quadro e da troca de dono, isoladas do banco com mocks. */
class BoardServiceTest {

    private final BoardRepository repo = mock(BoardRepository.class);
    private final BoardListRepository listRepo = mock(BoardListRepository.class);
    private final BoardMemberRepository memberRepo = mock(BoardMemberRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    // Os métodos de acesso são void: o mock não faz nada, ou seja, autoriza. É o que estes
    // testes querem — aqui a regra sob teste é a confirmação pelo nome, não a autorização.
    private final BoardAccess access = mock(BoardAccess.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final BoardService service =
            new BoardService(repo, listRepo, memberRepo, userRepo, access, currentUser);

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
        doThrow(new NotFoundException("Quadro 1 não encontrado")).when(access).requireOwner(1L);

        assertThatThrownBy(() -> service.delete(1L, "Estudos"))
                .isInstanceOf(NotFoundException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void transferirPosseParaQuemNaoEMembro_lancaNaoEncontrado() {
        Board board = new Board(DONO, "Estudos", null);
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepo.existsByBoardIdAndUserId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.transferOwnership(1L, 99L))
                .isInstanceOf(NotFoundException.class);

        assertThat(board.getOwner()).isSameAs(DONO);   // o quadro não trocou de mãos
    }

    @Test
    void transferirPosse_exigeSerODonoAtual() {
        doThrow(new ForbiddenException("Apenas o dono do quadro pode fazer isto"))
                .when(access).requireOwner(1L);

        assertThatThrownBy(() -> service.transferOwnership(1L, 99L))
                .isInstanceOf(ForbiddenException.class);

        verify(memberRepo, never()).existsByBoardIdAndUserId(any(), any());
    }

    @Test
    void transferirPosseParaMembro_trocaODono() {
        Board board = new Board(DONO, "Estudos", null);
        User novoDono = new User("Novo", "novo@planix.test", "hash");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepo.existsByBoardIdAndUserId(1L, 2L)).thenReturn(true);
        when(userRepo.getReferenceById(2L)).thenReturn(novoDono);

        service.transferOwnership(1L, 2L);

        assertThat(board.getOwner()).isSameAs(novoDono);
        verify(memberRepo, never()).delete(any());     // o dono antigo continua membro
    }
}
