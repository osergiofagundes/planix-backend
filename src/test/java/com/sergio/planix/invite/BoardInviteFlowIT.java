package com.sergio.planix.invite;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InvitePreviewResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.invite.dto.InviteResponse;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BoardInviteFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardInviteService inviteService;
    @Autowired BoardInviteRepository inviteRepo;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;

    @Test
    void aceitarConvite_daAcessoAoConteudoMasNaoAoQuadro() {
        BoardResponse quadro = boardService.create(new BoardRequest("Compartilhado", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, null));

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> boardService.get(quadro.id()))
                .isInstanceOf(NotFoundException.class);

        assertThat(inviteService.accept(convite.token()).id()).isEqualTo(quadro.id());

        assertThat(boardService.list()).extracting(BoardResponse::id).contains(quadro.id());

        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        cardService.create(lista.id(), new CardCreateRequest("Cartão do B"));

        assertThatThrownBy(() -> boardService.update(quadro.id(), new BoardRequest("Meu agora", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> boardService.delete(quadro.id(), "Compartilhado"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> inviteService.create(quadro.id(), new InviteRequest(null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void conviteDeUmUso_soServeParaUmaPessoa() {
        BoardResponse quadro = boardService.create(new BoardRequest("Um uso", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> inviteService.accept(convite.token()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Convite inválido ou expirado");
    }

    @Test
    void aceitarDuasVezes_naoGastaOSegundoUso() {
        BoardResponse quadro = boardService.create(new BoardRequest("Duplo clique", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());
        inviteService.accept(convite.token());

        assertThat(inviteRepo.findById(convite.id()).orElseThrow().getUses()).isEqualTo(1);
    }

    @Test
    void conviteRevogado_naoDeixaEntrarMasNaoExpulsaQuemJaEstaDentro() {
        BoardResponse quadro = boardService.create(new BoardRequest("Revogado", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 5));

        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());

        autenticarComo(usuarioLogado);
        inviteService.revoke(convite.id());

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> inviteService.accept(convite.token()))
                .isInstanceOf(NotFoundException.class);

        autenticarComo(b);
        assertThat(boardService.list()).extracting(BoardResponse::id).contains(quadro.id());
    }

    @Test
    void conviteExpirado_respondeIgualAoInexistente() {
        BoardResponse quadro = boardService.create(new BoardRequest("Expirado", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, null));

        BoardInvite armazenado = inviteRepo.findById(convite.id()).orElseThrow();
        armazenado.setExpiresAt(OffsetDateTime.now().minusDays(1));
        inviteRepo.save(armazenado);

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> inviteService.accept(convite.token()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Convite inválido ou expirado");
        assertThatThrownBy(() -> inviteService.accept("token-que-nunca-existiu"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Convite inválido ou expirado");
    }

    @Test
    void previewMostraOQuadroEQuemConvidou_semGastarUso() {
        BoardResponse quadro = boardService.create(new BoardRequest("Espiada", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));

        autenticarComo(criarUsuario());
        InvitePreviewResponse preview = inviteService.preview(convite.token());

        assertThat(preview.boardName()).isEqualTo("Espiada");
        assertThat(preview.invitedBy().id()).isEqualTo(usuarioLogado.getId());
        assertThat(inviteRepo.findById(convite.id()).orElseThrow().getUses()).isZero();
    }

    @Test
    void listagemDeConvites_naoTemOTokenEExigeSerODono() {
        BoardResponse quadro = boardService.create(new BoardRequest("Listagem", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 2));

        assertThat(inviteService.list(quadro.id()))
                .extracting(InviteResponse::id, InviteResponse::uses, InviteResponse::maxUses)
                .containsExactly(tuple(convite.id(), 0, 2));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());
        assertThatThrownBy(() -> inviteService.list(quadro.id()))
                .isInstanceOf(ForbiddenException.class);
    }
}
