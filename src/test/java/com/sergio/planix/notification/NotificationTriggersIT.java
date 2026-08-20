package com.sergio.planix.notification;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.comment.CommentService;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import com.sergio.planix.team.TeamMember;
import com.sergio.planix.team.TeamMemberRepository;
import com.sergio.planix.team.TeamRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class NotificationTriggersIT extends AuthenticatedIntegrationTest {

    @Autowired NotificationOutboxRepository outboxRepo;
    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;
    @Autowired CommentService commentService;
    @Autowired TeamInviteService inviteService;
    @Autowired TeamMemberRepository teamMemberRepo;
    @Autowired ObjectMapper mapper;

    private User colega;
    private Long fazendo;
    private Long revisao;
    private Long card;

    @BeforeEach
    void montaQuadroComDuasListas() {
        colega = criarUsuario();
        teamMemberRepo.save(new TeamMember(equipeDoTeste, colega, TeamRole.MEMBER));
        autenticarComo(usuarioLogado);

        Long quadro = boardService.create(quadroAberto("Sprint 12")).id();
        fazendo = listService.create(quadro, new BoardListRequest("Fazendo")).id();
        revisao = listService.create(quadro, new BoardListRequest("Revisão")).id();
        card = cardService.create(fazendo, new CardCreateRequest("Ajustar filtro")).id();

        // Os demais ITs comitam de verdade e deixam eventos aqui. Como este teste é
        // transacional, o deleteAll vale só para ele e volta atrás no rollback.
        outboxRepo.deleteAll();
    }

    @Test
    void moverCardDeLista_gravaCardMovedNaOutbox() {
        cardService.move(card, revisao, 0);

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.CARD_MOVED);
        assertThat(evento.recipients()).containsExactly(colega.getId());
        assertThat(evento.data()).containsEntry("fromList", "Fazendo")
                                 .containsEntry("toList", "Revisão");
    }

    @Test
    void moverCardDentroDaMesmaLista_naoGravaNada() {
        cardService.move(card, fazendo, 0);

        assertThat(pendentes()).isEmpty();
    }

    @Test
    void atribuirResponsavel_gravaCardAssignedSoParaEle() {
        assigneeService.assign(card, colega.getId());

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.CARD_ASSIGNED);
        assertThat(evento.recipients()).containsExactly(colega.getId());
    }

    @Test
    void atribuirDuasVezesAMesmaPessoa_gravaUmEventoSo() {
        assigneeService.assign(card, colega.getId());
        assigneeService.assign(card, colega.getId());

        assertThat(pendentes()).hasSize(1);
    }

    @Test
    void comentar_gravaCardCommentedParaOsResponsaveisMenosOAutor() {
        assigneeService.assign(card, colega.getId());
        outboxRepo.deleteAll();

        commentService.create(card, new CommentRequest("Revisar antes de fechar", null));

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.CARD_COMMENTED);
        assertThat(evento.recipients()).containsExactly(colega.getId());
    }

    @Test
    void aceitarConvite_gravaTeamMemberJoinedParaDonoEAdmins() {
        String token = inviteService.create(equipeDoTeste.getId(),
                new InviteRequest(null, null, TeamRole.MEMBER)).token();
        User novato = criarUsuario();
        autenticarComo(novato);

        inviteService.accept(token);

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.TEAM_MEMBER_JOINED);
        assertThat(evento.actor().id()).isEqualTo(novato.getId());
        assertThat(evento.recipients()).contains(usuarioLogado.getId());
        assertThat(evento.team().id()).isEqualTo(equipeDoTeste.getId());
    }

    @Test
    void aceitarConviteDeQuemJaEMembro_naoGravaNada() {
        String token = inviteService.create(equipeDoTeste.getId(),
                new InviteRequest(null, null, TeamRole.MEMBER)).token();
        autenticarComo(colega);

        inviteService.accept(token);

        assertThat(pendentes()).isEmpty();
    }

    private NotificationEvent unicoEvento() {
        List<NotificationOutbox> linhas = pendentes();
        assertThat(linhas).hasSize(1);
        return mapper.readValue(linhas.getFirst().getPayload(), NotificationEvent.class);
    }

    private List<NotificationOutbox> pendentes() {
        return outboxRepo.findBySentAtIsNullOrderByIdAsc(Limit.of(100));
    }
}
