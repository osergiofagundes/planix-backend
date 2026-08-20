package com.sergio.planix.notification;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardMemberService;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import com.sergio.planix.team.TeamMember;
import com.sergio.planix.team.TeamMemberRepository;
import com.sergio.planix.team.TeamRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientResolverIT extends AuthenticatedIntegrationTest {

    @Autowired RecipientResolver resolver;
    @Autowired BoardService boardService;
    @Autowired BoardMemberService boardMemberService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;
    @Autowired TeamMemberRepository teamMemberRepo;

    @Test
    void quadroAbertoAEquipe_todaAEquipeRecebeMenosOAtor() {
        User colega = entrarNaEquipe(TeamRole.MEMBER);
        Long quadro = boardService.create(quadroAberto("Sprint 12")).id();

        assertThat(resolver.doQuadro(quadro, usuarioLogado.getId()))
                .containsExactlyInAnyOrder(colega.getId());
    }

    @Test
    void quadroFechado_soDonoEMembrosExplicitosRecebem() {
        User colega = entrarNaEquipe(TeamRole.MEMBER);
        User deFora = entrarNaEquipe(TeamRole.MEMBER);
        Long quadro = boardService.create(quadroFechado("Confidencial")).id();
        boardMemberService.add(quadro, colega.getId());

        assertThat(resolver.doQuadro(quadro, null))
                .contains(usuarioLogado.getId(), colega.getId())
                .doesNotContain(deFora.getId());
    }

    @Test
    void quadroFechado_administradorDaEquipeRecebeMesmoSemSerMembroDoQuadro() {
        User admin = entrarNaEquipe(TeamRole.ADMIN);
        Long quadro = boardService.create(quadroFechado("Confidencial")).id();

        assertThat(resolver.doQuadro(quadro, null)).contains(admin.getId());
    }

    @Test
    void responsaveisDoCard_devolveOsAtribuidosMenosOAtor() {
        User colega = entrarNaEquipe(TeamRole.MEMBER);
        Long quadro = boardService.create(quadroAberto("Sprint 12")).id();
        Long lista = listService.create(quadro, new BoardListRequest("Fazendo")).id();
        Long card = cardService.create(lista, new CardCreateRequest("Ajustar filtro")).id();

        assigneeService.assign(card, colega.getId());
        assigneeService.assign(card, usuarioLogado.getId());

        assertThat(resolver.responsaveisDoCard(card, usuarioLogado.getId()))
                .containsExactly(colega.getId());
    }

    @Test
    void gestoresDaEquipe_devolveDonoEAdminsESemOsMembrosComuns() {
        User admin = entrarNaEquipe(TeamRole.ADMIN);
        User membro = entrarNaEquipe(TeamRole.MEMBER);

        assertThat(resolver.gestoresDaEquipe(equipeDoTeste.getId(), null))
                .contains(usuarioLogado.getId(), admin.getId())
                .doesNotContain(membro.getId());
    }

    @Test
    void gestoresDaEquipe_excluiOAtor() {
        entrarNaEquipe(TeamRole.ADMIN);

        assertThat(resolver.gestoresDaEquipe(equipeDoTeste.getId(), usuarioLogado.getId()))
                .doesNotContain(usuarioLogado.getId());
    }

    private User entrarNaEquipe(TeamRole papel) {
        User novo = criarUsuario();
        teamMemberRepo.save(new TeamMember(equipeDoTeste, novo, papel));
        return novo;
    }
}
