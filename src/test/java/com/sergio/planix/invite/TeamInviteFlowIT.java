package com.sergio.planix.invite;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InvitePreviewResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.invite.dto.InviteResponse;
import com.sergio.planix.team.TeamRole;
import com.sergio.planix.team.TeamService;
import com.sergio.planix.team.dto.TeamRequest;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class TeamInviteFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired TeamService teamService;
    @Autowired TeamInviteService inviteService;
    @Autowired TeamInviteRepository inviteRepo;

    private Long equipe() { return equipeDoTeste.getId(); }

    @Test
    void aceitarConvite_daAcessoAosQuadrosAbertosDaEquipeMasNaoAoComando() {
        BoardResponse quadro = boardService.create(quadroAberto("Compartilhado"));
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, null, null));

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> boardService.get(quadro.id()))
                .isInstanceOf(NotFoundException.class);

        assertThat(inviteService.accept(convite.token()).id()).isEqualTo(equipe());

        assertThat(boardService.list(null)).extracting(BoardResponse::id).contains(quadro.id());
        assertThatThrownBy(() -> teamService.update(equipe(), new TeamRequest("Minha agora", null, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> inviteService.create(equipe(), new InviteRequest(null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void quemEntraComoAdmin_jaAdministraAEquipe() {
        InviteCreatedResponse convite =
                inviteService.create(equipe(), new InviteRequest(null, 1, TeamRole.ADMIN));

        autenticarComo(criarUsuario());
        assertThat(inviteService.accept(convite.token()).myRole()).isEqualTo(TeamRole.ADMIN);
        assertThat(teamService.update(equipe(), new TeamRequest("Renomeada", null, null)).name())
                .isEqualTo("Renomeada");
    }

    @Test
    void naoDaParaConvidarAlguemComoDonoDaEquipe() {
        assertThatThrownBy(() ->
                inviteService.create(equipe(), new InviteRequest(null, 1, TeamRole.OWNER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void conviteDeUmUso_soServeParaUmaPessoa() {
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, 1, null));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> inviteService.accept(convite.token()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Convite inválido ou expirado");
    }

    @Test
    void aceitarDuasVezes_naoGastaOSegundoUso() {
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, 1, null));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());
        inviteService.accept(convite.token());

        assertThat(inviteRepo.findById(convite.id()).orElseThrow().getUses()).isEqualTo(1);
    }

    @Test
    void conviteRevogado_naoDeixaEntrarMasNaoExpulsaQuemJaEstaDentro() {
        BoardResponse quadro = boardService.create(quadroAberto("Revogado"));
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, 5, null));

        User b = criarUsuario();
        autenticarComo(b);
        inviteService.accept(convite.token());

        autenticarComo(usuarioLogado);
        inviteService.revoke(convite.id());

        autenticarComo(criarUsuario());
        assertThatThrownBy(() -> inviteService.accept(convite.token()))
                .isInstanceOf(NotFoundException.class);

        autenticarComo(b);
        assertThat(boardService.list(null)).extracting(BoardResponse::id).contains(quadro.id());
    }

    @Test
    void conviteExpirado_respondeIgualAoInexistente() {
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, null, null));

        TeamInvite armazenado = inviteRepo.findById(convite.id()).orElseThrow();
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
    void previewMostraAEquipeEQuemConvidou_semGastarUso() {
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, 1, null));
        String nomeDaEquipe = equipeDoTeste.getName();

        autenticarComo(criarUsuario());
        InvitePreviewResponse preview = inviteService.preview(convite.token());

        assertThat(preview.teamName()).isEqualTo(nomeDaEquipe);
        assertThat(preview.role()).isEqualTo(TeamRole.MEMBER);
        assertThat(preview.invitedBy().id()).isEqualTo(usuarioLogado.getId());
        assertThat(inviteRepo.findById(convite.id()).orElseThrow().getUses()).isZero();
    }

    @Test
    void listagemDeConvites_naoTemOTokenEExigeAdministrarAEquipe() {
        InviteCreatedResponse convite = inviteService.create(equipe(), new InviteRequest(null, 2, null));

        assertThat(inviteService.list(equipe()))
                .extracting(InviteResponse::id, InviteResponse::uses, InviteResponse::maxUses,
                        InviteResponse::role)
                .containsExactly(tuple(convite.id(), 0, 2, TeamRole.MEMBER));

        autenticarComo(criarUsuario());
        inviteService.accept(convite.token());
        assertThatThrownBy(() -> inviteService.list(equipe()))
                .isInstanceOf(ForbiddenException.class);
    }
}
