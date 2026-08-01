package com.sergio.planix.card;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.common.NotBoardMemberException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.history.dto.CardChangeResponse;
import com.sergio.planix.invite.BoardInviteService;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class CardAssigneeFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;
    @Autowired BoardInviteService inviteService;

    private Long cartaoDoQuadro(BoardResponse quadro, String titulo) {
        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        return cardService.create(lista.id(), new CardCreateRequest(titulo)).id();
    }

    private User membroDoQuadro(BoardResponse quadro) {
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));
        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return b;
    }

    @Test
    void atribuirMembro_apareceNoCartaoParaOsDoisEEhIdempotente() {
        BoardResponse quadro = boardService.create(new BoardRequest("Responsáveis", null));
        User b = membroDoQuadro(quadro);
        Long cartao = cartaoDoQuadro(quadro, "Comprar domínio");

        assigneeService.assign(cartao, b.getId());
        assigneeService.assign(cartao, b.getId());

        assertThat(cardService.get(cartao).assignees())
                .extracting(UserSummary::id)
                .containsExactly(b.getId());

        autenticarComo(b);
        assertThat(cardService.get(cartao).assignees())
                .extracting(UserSummary::id)
                .containsExactly(b.getId());
    }

    @Test
    void atribuirEDesatribuir_deixamUmaLinhaDeHistoricoCadaUm() {
        BoardResponse quadro = boardService.create(new BoardRequest("Histórico", null));
        User b = membroDoQuadro(quadro);
        Long cartao = cartaoDoQuadro(quadro, "Publicar site");

        assigneeService.assign(cartao, b.getId());
        assigneeService.assign(cartao, b.getId());
        assigneeService.unassign(cartao, b.getId());
        assigneeService.unassign(cartao, b.getId());

        assertThat(cardService.get(cartao).assignees()).isEmpty();
        assertThat(cardService.listChanges(cartao))
                .extracting(CardChangeResponse::field, CardChangeResponse::oldValue,
                        CardChangeResponse::newValue)
                .containsExactlyInAnyOrder(
                        tuple("assignee", null, String.valueOf(b.getId())),
                        tuple("assignee", String.valueOf(b.getId()), null));

        assertThat(cardService.listChanges(cartao))
                .extracting(CardChangeResponse::author)
                .extracting(UserSummary::id)
                .containsOnly(usuarioLogado.getId());
    }

    @Test
    void atribuirQuemNaoEMembro_ouIdQueNemExiste_daExatamenteAMesmaResposta() {
        BoardResponse quadro = boardService.create(new BoardRequest("Fechado", null));
        Long cartao = cartaoDoQuadro(quadro, "Só para membros");
        User c = criarUsuario();

        assertThatThrownBy(() -> assigneeService.assign(cartao, c.getId()))
                .isInstanceOf(NotBoardMemberException.class)
                .hasMessage("O usuário %d não é membro deste quadro".formatted(c.getId()));

        assertThatThrownBy(() -> assigneeService.assign(cartao, 999_999L))
                .isInstanceOf(NotBoardMemberException.class)
                .hasMessage("O usuário 999999 não é membro deste quadro");
    }

    @Test
    void quemNaoEMembroDoQuadro_nemDescobreQueOCartaoExiste() {
        BoardResponse quadro = boardService.create(new BoardRequest("Alheio", null));
        User b = membroDoQuadro(quadro);
        Long cartao = cartaoDoQuadro(quadro, "Cartão do A");

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> assigneeService.assign(cartao, b.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Cartão %d não encontrado".formatted(cartao));
        assertThatThrownBy(() -> assigneeService.unassign(cartao, b.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void doisResponsaveisNoMesmoCartao_convivemESaemUmDeCadaVez() {
        BoardResponse quadro = boardService.create(new BoardRequest("Dupla", null));
        User b = membroDoQuadro(quadro);
        Long cartao = cartaoDoQuadro(quadro, "Trabalho em dupla");

        assigneeService.assign(cartao, b.getId());
        assigneeService.assign(cartao, usuarioLogado.getId());

        assertThat(cardService.get(cartao).assignees()).hasSize(2);

        assigneeService.unassign(cartao, usuarioLogado.getId());
        assertThat(cardService.get(cartao).assignees())
                .extracting(UserSummary::id)
                .containsExactly(b.getId());
    }
}
