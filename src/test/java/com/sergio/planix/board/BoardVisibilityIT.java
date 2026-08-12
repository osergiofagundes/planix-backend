package com.sergio.planix.board;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import com.sergio.planix.team.TeamRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardVisibilityIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardMemberService boardMemberService;
    @Autowired TeamInviteService inviteService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;

    private User colega(TeamRole papel) {
        var convite = inviteService.create(equipeDoTeste.getId(), new InviteRequest(null, 1, papel));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    @Test
    void membroDaEquipe_entraNoQuadroAbertoENaoNoFechado() {
        BoardResponse aberto = boardService.create(quadroAberto("Comercial"));
        BoardResponse fechado = boardService.create(quadroFechado("Diretoria"));
        User funcionario = colega(TeamRole.MEMBER);

        autenticarComo(funcionario);
        assertThat(boardService.get(aberto.id()).id()).isEqualTo(aberto.id());
        assertThatThrownBy(() -> boardService.get(fechado.id()))
                .isInstanceOf(NotFoundException.class);
        assertThat(boardService.list(equipeDoTeste.getId()))
                .extracting(BoardResponse::id)
                .containsExactly(aberto.id());
    }

    @Test
    void quemNaoEDaEquipe_naoVeQuadroNenhumDela() {
        BoardResponse aberto = boardService.create(quadroAberto("Comercial"));
        BoardResponse fechado = boardService.create(quadroFechado("Diretoria"));

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> boardService.get(aberto.id())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> boardService.get(fechado.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void adminDaEquipe_enxergaEAdministraAteOsQuadrosFechadosQueNaoCriou() {
        BoardResponse fechado = boardService.create(quadroFechado("Diretoria"));
        User admin = colega(TeamRole.ADMIN);

        autenticarComo(admin);
        assertThat(boardService.get(fechado.id()).id()).isEqualTo(fechado.id());
        assertThat(boardService.update(fechado.id(), new BoardRequest("Renomeado", null)).name())
                .isEqualTo("Renomeado");
    }

    @Test
    void fecharUmQuadroAberto_tiraQuemSoEntravaPelaEquipe() {
        BoardResponse quadro = boardService.create(quadroAberto("Comercial"));
        User funcionario = colega(TeamRole.MEMBER);

        autenticarComo(funcionario);
        assertThat(boardService.get(quadro.id()).id()).isEqualTo(quadro.id());

        autenticarComo(usuarioLogado);
        boardService.update(quadro.id(),
                new BoardRequest("Comercial", null, null, BoardVisibility.RESTRICTED));

        autenticarComo(funcionario);
        assertThatThrownBy(() -> boardService.get(quadro.id())).isInstanceOf(NotFoundException.class);

        autenticarComo(usuarioLogado);
        assertThat(boardService.get(quadro.id()).visibility()).isEqualTo(BoardVisibility.RESTRICTED);
    }

    @Test
    void abrirUmQuadroFechado_devolveOAcessoAEquipeInteira() {
        BoardResponse quadro = boardService.create(quadroFechado("Diretoria"));
        User funcionario = colega(TeamRole.MEMBER);

        boardService.update(quadro.id(),
                new BoardRequest("Diretoria", null, null, BoardVisibility.TEAM));

        autenticarComo(funcionario);
        assertThat(boardService.get(quadro.id()).id()).isEqualTo(quadro.id());
    }

    @Test
    void emQuadroAberto_daParaAtribuirCartaoAQualquerUmDaEquipe() {
        BoardResponse quadro = boardService.create(quadroAberto("Comercial"));
        User funcionario = colega(TeamRole.MEMBER);
        var lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        Long cartao = cardService.create(lista.id(), new CardCreateRequest("Ligar para o cliente")).id();

        assigneeService.assign(cartao, funcionario.getId());

        assertThat(cardService.get(cartao).assignees())
                .extracting(com.sergio.planix.auth.dto.UserSummary::id)
                .containsExactly(funcionario.getId());
    }

    @Test
    void emQuadroFechado_soDaParaAtribuirAQuemFoiAdicionado() {
        BoardResponse quadro = boardService.create(quadroFechado("Diretoria"));
        User funcionario = colega(TeamRole.MEMBER);
        var lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        Long cartao = cardService.create(lista.id(), new CardCreateRequest("Fechar o balanço")).id();

        assertThatThrownBy(() -> assigneeService.assign(cartao, funcionario.getId()))
                .isInstanceOf(com.sergio.planix.common.exception.NotBoardMemberException.class);

        boardMemberService.add(quadro.id(), funcionario.getId());
        assigneeService.assign(cartao, funcionario.getId());

        assertThat(cardService.get(cartao).assignees()).hasSize(1);
    }
}
