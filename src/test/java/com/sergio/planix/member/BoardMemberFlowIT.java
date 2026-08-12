package com.sergio.planix.member;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.common.BoardOpenToTeamException;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.common.NotTeamMemberException;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardMemberFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardMemberService memberService;
    @Autowired TeamInviteService inviteService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;

    private User colegaDeEquipe() {
        InviteCreatedResponse convite =
                inviteService.create(equipeDoTeste.getId(), new InviteRequest(null, 1, null));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    private User membroDoQuadro(BoardResponse quadro) {
        User b = colegaDeEquipe();
        memberService.add(quadro.id(), b.getId());
        return b;
    }

    @Test
    void donoEntraComoMembroDoProprioQuadroFechado() {
        BoardResponse quadro = boardService.create(quadroFechado("Só meu"));

        assertThat(memberService.list(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactly(usuarioLogado.getId());
    }

    @Test
    void quadroAbertoAEquipe_listaAEquipeInteiraSemNinguemPrecisarSerAdicionado() {
        BoardResponse quadro = boardService.create(quadroAberto("Aberto"));
        User b = colegaDeEquipe();

        assertThat(memberService.list(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactlyInAnyOrder(usuarioLogado.getId(), b.getId());

        autenticarComo(b);
        assertThat(boardService.list(null)).extracting(BoardResponse::id).contains(quadro.id());
    }

    @Test
    void quadroFechado_soEnxergaQuemFoiAdicionado() {
        BoardResponse quadro = boardService.create(quadroFechado("Diretoria"));
        User b = colegaDeEquipe();

        autenticarComo(b);
        assertThat(boardService.list(null)).extracting(BoardResponse::id).doesNotContain(quadro.id());
        assertThatThrownBy(() -> boardService.get(quadro.id())).isInstanceOf(NotFoundException.class);

        autenticarComo(usuarioLogado);
        memberService.add(quadro.id(), b.getId());

        autenticarComo(b);
        assertThat(boardService.get(quadro.id()).id()).isEqualTo(quadro.id());
    }

    @Test
    void adicionarAoQuadro_soValeParaQuemJaEstaNaEquipe() {
        BoardResponse quadro = boardService.create(quadroFechado("Fechado"));
        User estranho = criarUsuario();

        assertThatThrownBy(() -> memberService.add(quadro.id(), estranho.getId()))
                .isInstanceOf(NotTeamMemberException.class);
    }

    @Test
    void adicionarEmQuadroAbertoAEquipe_naoFazSentidoEDa409() {
        BoardResponse quadro = boardService.create(quadroAberto("Todos"));
        User b = colegaDeEquipe();

        assertThatThrownBy(() -> memberService.add(quadro.id(), b.getId()))
                .isInstanceOf(BoardOpenToTeamException.class);
        assertThatThrownBy(() -> memberService.remove(quadro.id(), b.getId()))
                .isInstanceOf(BoardOpenToTeamException.class);

        autenticarComo(b);
        assertThatThrownBy(() -> memberService.leave(quadro.id()))
                .isInstanceOf(BoardOpenToTeamException.class);
    }

    @Test
    void adicionarDuasVezes_eInofensivo() {
        BoardResponse quadro = boardService.create(quadroFechado("Repetido"));
        User b = colegaDeEquipe();

        memberService.add(quadro.id(), b.getId());
        memberService.add(quadro.id(), b.getId());

        assertThat(memberService.list(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactly(usuarioLogado.getId(), b.getId());
    }

    @Test
    void candidatos_saoOsDaEquipeQueAindaNaoEstaoNoQuadro() {
        BoardResponse quadro = boardService.create(quadroFechado("Convocação"));
        User b = colegaDeEquipe();

        assertThat(memberService.candidates(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactly(b.getId());

        memberService.add(quadro.id(), b.getId());
        assertThat(memberService.candidates(quadro.id())).isEmpty();
    }

    @Test
    void removerMembro_tiraOAcessoMasNaoOConteudoQueElePublicou() {
        BoardResponse quadro = boardService.create(quadroFechado("Rotativo"));
        User b = membroDoQuadro(quadro);

        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        autenticarComo(b);
        CardResponse cartaoDoB = cardService.create(lista.id(), new CardCreateRequest("Cartão do B"));

        autenticarComo(usuarioLogado);
        memberService.remove(quadro.id(), b.getId());

        autenticarComo(b);
        assertThat(boardService.list(null)).extracting(BoardResponse::id).doesNotContain(quadro.id());
        assertThatThrownBy(() -> boardService.get(quadro.id())).isInstanceOf(NotFoundException.class);

        autenticarComo(usuarioLogado);
        assertThat(cardService.get(cartaoDoB.id()).title()).isEqualTo("Cartão do B");
    }

    @Test
    void membroSai_sozinho() {
        BoardResponse quadro = boardService.create(quadroFechado("Porta aberta"));
        User b = membroDoQuadro(quadro);

        autenticarComo(b);
        memberService.leave(quadro.id());

        assertThat(boardService.list(null)).extracting(BoardResponse::id).doesNotContain(quadro.id());
    }

    @Test
    void oDonoNaoSaiNemPodeSerRemovido() {
        BoardResponse quadro = boardService.create(quadroFechado("Sem saída"));
        User b = membroDoQuadro(quadro);

        assertThatThrownBy(() -> memberService.leave(quadro.id()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> memberService.remove(quadro.id(), usuarioLogado.getId()))
                .isInstanceOf(ForbiddenException.class);

        autenticarComo(b);
        assertThatThrownBy(() -> memberService.remove(quadro.id(), usuarioLogado.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removerQuemNaoEMembro_respondeNaoEncontrado() {
        BoardResponse quadro = boardService.create(quadroFechado("Vazio"));
        User b = colegaDeEquipe();

        assertThatThrownBy(() -> memberService.remove(quadro.id(), b.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void transferirPosse_trocaQuemMandaEMantemODonoAntigoComoMembro() {
        BoardResponse quadro = boardService.create(quadroFechado("Passa o bastão"));
        User b = membroDoQuadro(quadro);

        BoardResponse depois = boardService.transferOwnership(quadro.id(), b.getId());
        assertThat(depois.owner().id()).isEqualTo(b.getId());

        assertThat(boardService.list(null)).extracting(BoardResponse::id).contains(quadro.id());

        autenticarComo(b);
        assertThat(boardService.update(quadro.id(), new BoardRequest("Do B agora", null)).name())
                .isEqualTo("Do B agora");
    }

    @Test
    void quemNaoAdministraNadaNaoConfiguraOQuadro() {
        BoardResponse quadro = boardService.create(quadroFechado("Meu"));
        User b = membroDoQuadro(quadro);

        autenticarComo(b);
        assertThatThrownBy(() -> boardService.update(quadro.id(), new BoardRequest("Meu agora", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> boardService.delete(quadro.id(), "Meu"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removerMembro_tiraONomeDeleDosCartoesEmQueEraResponsavel() {
        BoardResponse quadro = boardService.create(quadroFechado("Faxina"));
        User b = membroDoQuadro(quadro);
        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));

        List<Long> cartoes = List.of(
                cardService.create(lista.id(), new CardCreateRequest("Um")).id(),
                cardService.create(lista.id(), new CardCreateRequest("Dois")).id(),
                cardService.create(lista.id(), new CardCreateRequest("Três")).id());
        cartoes.forEach(id -> assigneeService.assign(id, b.getId()));

        memberService.remove(quadro.id(), b.getId());

        for (Long id : cartoes) {
            CardResponse cartao = cardService.get(id);
            assertThat(cartao.assignees()).isEmpty();
            assertThat(cartao.title()).isNotBlank();
        }
    }

    @Test
    void membroQueSaiSozinho_tambemDeixaDeSerResponsavel() {
        BoardResponse quadro = boardService.create(quadroFechado("Saída"));
        User b = membroDoQuadro(quadro);
        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        Long cartao = cardService.create(lista.id(), new CardCreateRequest("Sozinho")).id();
        assigneeService.assign(cartao, b.getId());

        autenticarComo(b);
        memberService.leave(quadro.id());

        autenticarComo(usuarioLogado);
        assertThat(cardService.get(cartao).assignees()).isEmpty();
    }

    @Test
    void transferirPosseParaQuemNaoTemAcesso_respondeNaoEncontrado() {
        BoardResponse quadro = boardService.create(quadroFechado("Estranho"));
        User estranho = criarUsuario();

        assertThatThrownBy(() -> boardService.transferOwnership(quadro.id(), estranho.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
