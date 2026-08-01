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
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.invite.BoardInviteService;
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
    @Autowired BoardInviteService inviteService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;

    private User quadroComMembro(BoardResponse quadro) {
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    @Test
    void donoEntraComoMembroDoProprioQuadro() {
        BoardResponse quadro = boardService.create(new BoardRequest("Só meu", null));

        assertThat(memberService.list(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactly(usuarioLogado.getId());
    }

    @Test
    void qualquerMembroVeQuemMaisEstaNoQuadro() {
        BoardResponse quadro = boardService.create(new BoardRequest("Time", null));
        User b = quadroComMembro(quadro);

        autenticarComo(b);
        assertThat(memberService.list(quadro.id()))
                .extracting(UserSummary::id)
                .containsExactly(usuarioLogado.getId(), b.getId());
    }

    @Test
    void removerMembro_tiraOAcessoMasNaoOConteudoQueElePublicou() {
        BoardResponse quadro = boardService.create(new BoardRequest("Rotativo", null));
        User b = quadroComMembro(quadro);

        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        autenticarComo(b);
        CardResponse cartaoDoB = cardService.create(lista.id(), new CardCreateRequest("Cartão do B"));

        autenticarComo(usuarioLogado);
        memberService.remove(quadro.id(), b.getId());

        autenticarComo(b);
        assertThat(boardService.list()).extracting(BoardResponse::id).doesNotContain(quadro.id());
        assertThatThrownBy(() -> boardService.get(quadro.id())).isInstanceOf(NotFoundException.class);

        autenticarComo(usuarioLogado);
        assertThat(cardService.get(cartaoDoB.id()).title()).isEqualTo("Cartão do B");
    }

    @Test
    void membroSai_sozinho() {
        BoardResponse quadro = boardService.create(new BoardRequest("Porta aberta", null));
        User b = quadroComMembro(quadro);

        autenticarComo(b);
        memberService.leave(quadro.id());

        assertThat(boardService.list()).extracting(BoardResponse::id).doesNotContain(quadro.id());
    }

    @Test
    void oDonoNaoSaiNemPodeSerRemovido() {
        BoardResponse quadro = boardService.create(new BoardRequest("Sem saída", null));
        User b = quadroComMembro(quadro);

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
        BoardResponse quadro = boardService.create(new BoardRequest("Vazio", null));
        User estranho = criarUsuario();

        assertThatThrownBy(() -> memberService.remove(quadro.id(), estranho.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void transferirPosse_trocaQuemMandaEMantemODonoAntigoComoMembro() {
        BoardResponse quadro = boardService.create(new BoardRequest("Passa o bastão", null));
        User b = quadroComMembro(quadro);

        BoardResponse depois = boardService.transferOwnership(quadro.id(), b.getId());
        assertThat(depois.owner().id()).isEqualTo(b.getId());

        assertThat(boardService.list()).extracting(BoardResponse::id).contains(quadro.id());
        assertThatThrownBy(() -> boardService.update(quadro.id(), new BoardRequest("Meu de novo", null)))
                .isInstanceOf(ForbiddenException.class);

        autenticarComo(b);
        assertThat(boardService.update(quadro.id(), new BoardRequest("Do B agora", null)).name())
                .isEqualTo("Do B agora");
        memberService.remove(quadro.id(), usuarioLogado.getId());
    }

    @Test
    void removerMembro_tiraONomeDeleDosCartoesEmQueEraResponsavel() {
        BoardResponse quadro = boardService.create(new BoardRequest("Faxina", null));
        User b = quadroComMembro(quadro);
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
        BoardResponse quadro = boardService.create(new BoardRequest("Saída", null));
        User b = quadroComMembro(quadro);
        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        Long cartao = cardService.create(lista.id(), new CardCreateRequest("Sozinho")).id();
        assigneeService.assign(cartao, b.getId());

        autenticarComo(b);
        memberService.leave(quadro.id());

        autenticarComo(usuarioLogado);
        assertThat(cardService.get(cartao).assignees()).isEmpty();
    }

    @Test
    void transferirPosseParaQuemNaoEMembro_respondeNaoEncontrado() {
        BoardResponse quadro = boardService.create(new BoardRequest("Estranho", null));
        User estranho = criarUsuario();

        assertThatThrownBy(() -> boardService.transferOwnership(quadro.id(), estranho.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
