package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListRepository;
import com.sergio.planix.member.BoardMemberRepository;
import com.sergio.planix.team.Team;
import com.sergio.planix.team.TeamAccess;
import com.sergio.planix.team.TeamRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class BoardServiceTest {

    private final BoardRepository repo = mock(BoardRepository.class);
    private final BoardListRepository listRepo = mock(BoardListRepository.class);
    private final BoardMemberRepository memberRepo = mock(BoardMemberRepository.class);
    private final TeamRepository teamRepo = mock(TeamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final BoardAccess access = mock(BoardAccess.class);
    private final TeamAccess teamAccess = mock(TeamAccess.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final BoardService service = new BoardService(repo, listRepo, memberRepo, teamRepo,
            userRepo, access, teamAccess, currentUser);

    private static final User DONO = new User("Dono", "dono@planix.test", "hash");
    private static final Team EQUIPE = new Team(DONO, "Acme", null, null);

    private static Board quadro(String nome) {
        return new Board(EQUIPE, DONO, nome, null, null, BoardVisibility.TEAM);
    }

    @Test
    void excluirQuadroComConteudoSemConfirmacao_lancaExcecao() {
        Board board = quadro("Estudos");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, null))
                .isInstanceOf(BoardNotEmptyException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void excluirQuadroComConteudoComNomeErrado_lancaExcecao() {
        Board board = quadro("Estudos");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, "estudos"))
                .isInstanceOf(BoardNotEmptyException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void excluirQuadroComConteudoComNomeCorreto_apaga() {
        Board board = quadro("Estudos");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(true);

        service.delete(1L, "Estudos");

        verify(repo).delete(board);
    }

    @Test
    void excluirQuadroVazio_naoExigeConfirmacao() {
        Board board = quadro("Estudos");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(listRepo.existsByBoardId(1L)).thenReturn(false);

        service.delete(1L, null);

        verify(repo).delete(board);
    }

    @Test
    void excluirQuadro_exigeAdministrarOQuadro() {
        doThrow(new NotFoundException("Quadro 1 não encontrado")).when(access).requireManager(1L);

        assertThatThrownBy(() -> service.delete(1L, "Estudos"))
                .isInstanceOf(NotFoundException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void transferirPosseParaQuemNaoTemAcesso_lancaNaoEncontrado() {
        Board board = quadro("Estudos");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(access.isMember(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.transferOwnership(1L, 99L))
                .isInstanceOf(NotFoundException.class);

        assertThat(board.getOwner()).isSameAs(DONO);
    }

    @Test
    void transferirPosse_exigeAdministrarOQuadro() {
        doThrow(new ForbiddenException("Apenas o dono do quadro ou quem administra a equipe pode fazer isto"))
                .when(access).requireManager(1L);

        assertThatThrownBy(() -> service.transferOwnership(1L, 99L))
                .isInstanceOf(ForbiddenException.class);

        verify(access, never()).isMember(anyLong(), anyLong());
    }

    @Test
    void transferirPosseParaQuemTemAcesso_trocaODono() {
        Board board = quadro("Estudos");
        User novoDono = new User("Novo", "novo@planix.test", "hash");
        when(repo.findById(1L)).thenReturn(Optional.of(board));
        when(access.isMember(1L, 2L)).thenReturn(true);
        when(userRepo.getReferenceById(2L)).thenReturn(novoDono);

        service.transferOwnership(1L, 2L);

        assertThat(board.getOwner()).isSameAs(novoDono);
        verify(memberRepo, never()).delete(any());
    }
}
