package com.sergio.planix.team;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.member.BoardMemberService;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import com.sergio.planix.team.dto.TeamMemberResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class TeamMemberFlowIT extends AuthenticatedIntegrationTest {

    @Autowired TeamMemberService memberService;
    @Autowired TeamInviteService inviteService;
    @Autowired BoardService boardService;
    @Autowired BoardMemberService boardMemberService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;

    private Long equipe() { return equipeDoTeste.getId(); }

    private User colega(TeamRole papel) {
        var convite = inviteService.create(equipe(), new InviteRequest(null, 1, papel));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    @Test
    void listaTrazOPapelDeCadaUmNaOrdemDeEntrada() {
        User b = colega(TeamRole.MEMBER);

        assertThat(memberService.list(equipe()))
                .extracting(m -> m.user().id(), TeamMemberResponse::role)
                .containsExactly(
                        tuple(usuarioLogado.getId(), TeamRole.OWNER),
                        tuple(b.getId(), TeamRole.MEMBER));
    }

    @Test
    void oDonoPromoveERebaixa() {
        User b = colega(TeamRole.MEMBER);

        assertThat(memberService.changeRole(equipe(), b.getId(), TeamRole.ADMIN).role())
                .isEqualTo(TeamRole.ADMIN);
        assertThat(memberService.changeRole(equipe(), b.getId(), TeamRole.MEMBER).role())
                .isEqualTo(TeamRole.MEMBER);
    }

    @Test
    void promoverAlguemADono_soPelaTransferenciaDePosse() {
        User b = colega(TeamRole.MEMBER);

        assertThatThrownBy(() -> memberService.changeRole(equipe(), b.getId(), TeamRole.OWNER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminNaoMudaPapelDeNinguem() {
        User admin = colega(TeamRole.ADMIN);
        User membro = colega(TeamRole.MEMBER);

        autenticarComo(admin);
        assertThatThrownBy(() -> memberService.changeRole(equipe(), membro.getId(), TeamRole.ADMIN))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminRemoveMembro_masNaoOutroAdminNemODono() {
        User admin = colega(TeamRole.ADMIN);
        User outroAdmin = colega(TeamRole.ADMIN);
        User membro = colega(TeamRole.MEMBER);

        autenticarComo(admin);
        assertThatThrownBy(() -> memberService.remove(equipe(), usuarioLogado.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> memberService.remove(equipe(), outroAdmin.getId()))
                .isInstanceOf(ForbiddenException.class);

        memberService.remove(equipe(), membro.getId());
        assertThat(memberService.list(equipe()))
                .extracting(m -> m.user().id())
                .doesNotContain(membro.getId());
    }

    @Test
    void oDonoNaoSaiDaPropriaEquipe() {
        colega(TeamRole.ADMIN);

        assertThatThrownBy(() -> memberService.leave(equipe()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sairDaEquipe_levaJuntoOAcessoAosQuadrosEAsAtribuicoes() {
        User b = colega(TeamRole.MEMBER);

        BoardResponse aberto = boardService.create(quadroAberto("Comercial"));
        BoardResponse fechado = boardService.create(quadroFechado("Diretoria"));
        boardMemberService.add(fechado.id(), b.getId());

        var lista = listService.create(aberto.id(), new BoardListRequest("A Fazer"));
        Long cartao = cardService.create(lista.id(), new CardCreateRequest("Tarefa")).id();
        assigneeService.assign(cartao, b.getId());

        autenticarComo(b);
        memberService.leave(equipe());

        assertThat(boardService.list(null))
                .extracting(BoardResponse::id)
                .doesNotContain(aberto.id(), fechado.id());
        assertThatThrownBy(() -> boardService.get(fechado.id())).isInstanceOf(NotFoundException.class);

        autenticarComo(usuarioLogado);
        assertThat(cardService.get(cartao).assignees()).isEmpty();
        assertThat(boardMemberService.list(fechado.id()))
                .extracting(UserSummary::id)
                .containsExactly(usuarioLogado.getId());
    }

    @Test
    void removerDaEquipe_temOMesmoEfeitoQueSair() {
        User b = colega(TeamRole.MEMBER);
        BoardResponse quadro = boardService.create(quadroAberto("Compartilhado"));

        memberService.remove(equipe(), b.getId());

        autenticarComo(b);
        assertThat(boardService.list(null)).extracting(BoardResponse::id).doesNotContain(quadro.id());
    }

    @Test
    void removerQuemNaoEDaEquipe_respondeNaoEncontrado() {
        User estranho = criarUsuario();

        assertThatThrownBy(() -> memberService.remove(equipe(), estranho.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void quemNaoEDaEquipeNemVeAListaDeMembros() {
        autenticarComo(criarUsuario());

        assertThatThrownBy(() -> memberService.list(equipe()))
                .isInstanceOf(NotFoundException.class);
    }
}
