package com.sergio.planix.team;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardCreateRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.exception.ForbiddenException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.common.exception.TeamNotEmptyException;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.board.BoardMemberService;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import com.sergio.planix.team.dto.TeamRequest;
import com.sergio.planix.team.dto.TeamResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamFlowIT extends AuthenticatedIntegrationTest {

    @Autowired TeamService teamService;
    @Autowired TeamMemberService memberService;
    @Autowired TeamInviteService inviteService;
    @Autowired BoardService boardService;
    @Autowired BoardMemberService boardMemberService;
    @Autowired BoardListService listService;

    private User colegaDeEquipe(Long teamId, TeamRole papel) {
        var convite = inviteService.create(teamId, new InviteRequest(null, 1, papel));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    @Test
    void aContaNasceComUmaEquipePropria() {
        assertThat(teamService.list())
                .extracting(TeamResponse::id, TeamResponse::myRole)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(equipeDoTeste.getId(), TeamRole.OWNER));
    }

    @Test
    void criarEquipe_deixaQuemCriouComoDono() {
        TeamResponse nova = teamService.create(new TeamRequest("Acme", "A empresa toda", "building-2"));

        assertThat(nova.myRole()).isEqualTo(TeamRole.OWNER);
        assertThat(nova.owner().id()).isEqualTo(usuarioLogado.getId());
        assertThat(teamService.get(nova.id()).name()).isEqualTo("Acme");
        assertThat(teamService.list()).extracting(TeamResponse::id).contains(nova.id());
    }

    @Test
    void equipeDeOutraPessoa_respondeNaoEncontrada() {
        TeamResponse minha = teamService.create(new TeamRequest("Reservada", null, null));

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> teamService.get(minha.id())).isInstanceOf(NotFoundException.class);
        assertThat(teamService.list()).extracting(TeamResponse::id).doesNotContain(minha.id());
    }

    @Test
    void adminRenomeiaAEquipe_masSoODonoExcluiOuTransfere() {
        Long equipe = equipeDoTeste.getId();
        User admin = colegaDeEquipe(equipe, TeamRole.ADMIN);

        autenticarComo(admin);
        assertThat(teamService.update(equipe, new TeamRequest("Renomeada", null, null)).name())
                .isEqualTo("Renomeada");
        assertThatThrownBy(() -> teamService.delete(equipe, "Renomeada"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> teamService.transferOwnership(equipe, admin.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void membroSimplesNaoConfiguraAEquipe() {
        Long equipe = equipeDoTeste.getId();
        User membro = colegaDeEquipe(equipe, TeamRole.MEMBER);

        autenticarComo(membro);
        assertThatThrownBy(() -> teamService.update(equipe, new TeamRequest("Minha", null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void transferirPosse_promoveOOutroERebaixaVoceAAdmin() {
        Long equipe = equipeDoTeste.getId();
        User b = colegaDeEquipe(equipe, TeamRole.MEMBER);

        TeamResponse depois = teamService.transferOwnership(equipe, b.getId());

        assertThat(depois.owner().id()).isEqualTo(b.getId());
        assertThat(depois.myRole()).isEqualTo(TeamRole.ADMIN);
        assertThatThrownBy(() -> teamService.delete(equipe, null))
                .isInstanceOf(ForbiddenException.class);

        autenticarComo(b);
        assertThat(teamService.get(equipe).myRole()).isEqualTo(TeamRole.OWNER);
    }

    @Test
    void transferirPosseParaQuemNaoEDaEquipe_respondeNaoEncontrado() {
        User estranho = criarUsuario();

        assertThatThrownBy(() -> teamService.transferOwnership(equipeDoTeste.getId(), estranho.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void excluirEquipeComQuadros_exigeONomeEDepoisLevaOsQuadrosJunto() {
        TeamResponse equipe = teamService.create(new TeamRequest("Descartável", null, null));
        BoardResponse quadro = boardService.create(
                new BoardCreateRequest(equipe.id(), "Um quadro", null, null, null));
        listService.create(quadro.id(), new BoardListRequest("A Fazer"));

        assertThatThrownBy(() -> teamService.delete(equipe.id(), null))
                .isInstanceOf(TeamNotEmptyException.class);
        assertThatThrownBy(() -> teamService.delete(equipe.id(), "descartável"))
                .isInstanceOf(TeamNotEmptyException.class);

        teamService.delete(equipe.id(), "Descartável");

        assertThat(teamService.list()).extracting(TeamResponse::id).doesNotContain(equipe.id());
        assertThatThrownBy(() -> boardService.get(quadro.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void excluirEquipeVazia_naoPedeConfirmacao() {
        TeamResponse equipe = teamService.create(new TeamRequest("Vazia", null, null));

        teamService.delete(equipe.id(), null);

        assertThat(teamService.list()).extracting(TeamResponse::id).doesNotContain(equipe.id());
    }

    @Test
    void criarQuadroEmEquipeDeOutro_respondeNaoEncontrado() {
        TeamResponse minha = teamService.create(new TeamRequest("Fechada", null, null));

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> boardService.create(
                new BoardCreateRequest(minha.id(), "Intruso", null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void quadrosSaoFiltradosPelaEquipe() {
        TeamResponse outra = teamService.create(new TeamRequest("Outra", null, null));
        BoardResponse daPrimeira = boardService.create(quadroAberto("Da primeira"));
        BoardResponse daOutra = boardService.create(
                new BoardCreateRequest(outra.id(), "Da outra", null, null, null));

        assertThat(boardService.list(equipeDoTeste.getId()))
                .extracting(BoardResponse::id).containsExactly(daPrimeira.id());
        assertThat(boardService.list(outra.id()))
                .extracting(BoardResponse::id).containsExactly(daOutra.id());
        assertThat(boardService.list(null))
                .extracting(BoardResponse::id).contains(daPrimeira.id(), daOutra.id());
    }

    @Test
    void quadroNasceAbertoAEquipeQuandoAVisibilidadeNaoEDita() {
        BoardResponse quadro = boardService.create(
                new BoardCreateRequest(equipeDoTeste.getId(), "Padrão", null, null, null));

        assertThat(quadro.visibility()).isEqualTo(com.sergio.planix.board.BoardVisibility.TEAM);
        assertThat(quadro.teamId()).isEqualTo(equipeDoTeste.getId());
    }

    @Test
    void listaDeEquipesTrazOPapelDeCadaUma() {
        Long minha = equipeDoTeste.getId();
        User b = colegaDeEquipe(minha, TeamRole.ADMIN);

        autenticarComo(b);
        assertThat(teamService.list())
                .filteredOn(t -> t.id().equals(minha))
                .extracting(TeamResponse::myRole)
                .containsExactly(TeamRole.ADMIN);

        assertThat(teamService.list())
                .filteredOn(t -> !t.id().equals(minha))
                .extracting(TeamResponse::myRole)
                .containsExactly(TeamRole.OWNER);
    }

    @Test
    void aListagemContaOsMembrosEOsQuadrosQueCadaUmEnxerga() {
        Long equipe = equipeDoTeste.getId();
        boardService.create(quadroAberto("Comercial"));
        boardService.create(quadroAberto("Marketing"));
        BoardResponse fechado = boardService.create(quadroFechado("Diretoria"));

        User socio = colegaDeEquipe(equipe, TeamRole.MEMBER);
        User funcionario = colegaDeEquipe(equipe, TeamRole.MEMBER);
        boardMemberService.add(fechado.id(), socio.getId());

        assertThat(contagemDe(equipe))
                .extracting(TeamResponse::memberCount, TeamResponse::boardCount)
                .containsExactly(3L, 3L);

        autenticarComo(socio);
        assertThat(contagemDe(equipe))
                .extracting(TeamResponse::memberCount, TeamResponse::boardCount)
                .containsExactly(3L, 3L);

        autenticarComo(funcionario);
        assertThat(contagemDe(equipe))
                .extracting(TeamResponse::memberCount, TeamResponse::boardCount)
                .containsExactly(3L, 2L);
    }

    @Test
    void equipeRecemCriada_temUmMembroENenhumQuadro() {
        TeamResponse nova = teamService.create(new TeamRequest("Zerada", null, null));

        assertThat(nova.memberCount()).isEqualTo(1);
        assertThat(nova.boardCount()).isZero();
        assertThat(contagemDe(nova.id()).boardCount()).isZero();
    }

    private TeamResponse contagemDe(Long teamId) {
        return teamService.list().stream()
                .filter(t -> t.id().equals(teamId))
                .findFirst().orElseThrow();
    }

    @Test
    void membroNaoRemoveNinguemDaEquipe() {
        Long equipe = equipeDoTeste.getId();
        User membro = colegaDeEquipe(equipe, TeamRole.MEMBER);

        autenticarComo(membro);
        assertThatThrownBy(() -> memberService.remove(equipe, usuarioLogado.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}
