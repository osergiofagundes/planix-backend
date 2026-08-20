package com.sergio.planix.notification;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAssigneeService;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
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

// Transacional porque e assim que o publisher e usado: sempre de dentro da transacao de um
// service. Fora dela os proxies lazy de User e BoardList nem inicializam.
@Transactional
class NotificationPublisherIT extends AuthenticatedIntegrationTest {

    @Autowired NotificationPublisher publisher;
    @Autowired NotificationOutboxRepository outboxRepo;
    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;
    @Autowired CardRepository cardRepo;
    @Autowired TeamMemberRepository teamMemberRepo;
    @Autowired ObjectMapper mapper;

    private User colega;
    private Card card;

    @BeforeEach
    void montaQuadroComUmColega() {
        colega = criarUsuario();
        teamMemberRepo.save(new TeamMember(equipeDoTeste, colega, TeamRole.MEMBER));
        autenticarComo(usuarioLogado);

        Long quadro = boardService.create(quadroAberto("Sprint 12")).id();
        Long lista = listService.create(quadro, new BoardListRequest("Fazendo")).id();
        card = cardRepo.findById(
                cardService.create(lista, new CardCreateRequest("Ajustar filtro de cards")).id())
                .orElseThrow();

        // Os demais ITs comitam de verdade e deixam eventos aqui. Como este teste é
        // transacional, o deleteAll vale só para ele e volta atrás no rollback.
        outboxRepo.deleteAll();
    }

    @Test
    void cardMoved_gravaUmaLinhaNaOutboxComOPayloadDoContrato() {
        publisher.cardMoved(card, "Fazendo", "Revisão");

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.CARD_MOVED);
        assertThat(evento.recipients()).containsExactly(colega.getId());
        assertThat(evento.actor().id()).isEqualTo(usuarioLogado.getId());
        assertThat(evento.actor().name()).isEqualTo(usuarioLogado.getName());
        assertThat(evento.board().name()).isEqualTo("Sprint 12");
        assertThat(evento.card().name()).isEqualTo("Ajustar filtro de cards");
        assertThat(evento.team()).isNull();
        assertThat(evento.data()).containsEntry("fromList", "Fazendo")
                                 .containsEntry("toList", "Revisão");
    }

    @Test
    void cardAssigned_notificaSoOResponsavelAdicionado() {
        publisher.cardAssigned(card, colega.getId());

        assertThat(unicoEvento().recipients()).containsExactly(colega.getId());
    }

    @Test
    void atribuirASiMesmo_naoGravaNadaNaOutbox() {
        publisher.cardAssigned(card, usuarioLogado.getId());

        assertThat(pendentes()).isEmpty();
    }

    @Test
    void cardCommented_notificaOsResponsaveisMenosOAutorEGuardaOTrecho() {
        assigneeService.assign(card.getId(), colega.getId());
        assigneeService.assign(card.getId(), usuarioLogado.getId());
        outboxRepo.deleteAll();   // atribuir já notifica; aqui só interessa o comentário

        publisher.cardCommented(card, "Isto aqui precisa de revisão antes de fechar");

        NotificationEvent evento = unicoEvento();
        assertThat(evento.recipients()).containsExactly(colega.getId());
        assertThat(evento.data().get("excerpt")).startsWith("Isto aqui precisa");
    }

    @Test
    void quadroSemNinguemAlemDoAtor_naoGravaNadaNaOutbox() {
        teamMemberRepo.deleteByTeamIdAndUserId(equipeDoTeste.getId(), colega.getId());

        publisher.cardMoved(card, "Fazendo", "Revisão");

        assertThat(pendentes()).isEmpty();
    }

    @Test
    void teamMemberJoined_notificaGestoresComAChaveDaEquipeEmVezDeQuadro() {
        autenticarComo(colega);

        publisher.teamMemberJoined(equipeDoTeste);

        NotificationEvent evento = unicoEvento();
        assertThat(evento.type()).isEqualTo(NotificationType.TEAM_MEMBER_JOINED);
        assertThat(evento.recipients()).containsExactly(usuarioLogado.getId());
        assertThat(evento.board()).isNull();
        assertThat(evento.kafkaKey()).isEqualTo("team:" + equipeDoTeste.getId());
    }

    @Test
    void cardDueSoon_gravaSemAtorEComIdDeterministicoQueNaoDuplica() {
        card.setDueDate(java.time.OffsetDateTime.now().plusHours(6));
        assigneeService.assign(card.getId(), colega.getId());
        cardRepo.saveAndFlush(card);
        outboxRepo.deleteAll();   // atribuir já notifica; aqui só interessa o prazo

        publisher.cardDueSoon(card);
        publisher.cardDueSoon(card);

        NotificationEvent evento = unicoEvento();
        assertThat(evento.actor()).isNull();
        assertThat(evento.type()).isEqualTo(NotificationType.CARD_DUE_SOON);
        assertThat(evento.recipients()).containsExactly(colega.getId());
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
