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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Reabilita o bean do scanner, que a base dos ITs desliga. Quem dispara a varredura aqui é
 * o teste: o agendador do Spring fica desligado na suíte inteira.
 */
@Transactional
@TestPropertySource(properties = "planix.due-scan.enabled=true")
class DueDateScannerIT extends AuthenticatedIntegrationTest {

    @Autowired DueDateScanner scanner;
    @Autowired NotificationOutboxRepository outboxRepo;
    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CardAssigneeService assigneeService;
    @Autowired CardRepository cardRepo;
    @Autowired TeamMemberRepository teamMemberRepo;
    @Autowired ObjectMapper mapper;

    private User colega;
    private Long lista;

    @BeforeEach
    void montaQuadro() {
        colega = criarUsuario();
        teamMemberRepo.save(new TeamMember(equipeDoTeste, colega, TeamRole.MEMBER));
        autenticarComo(usuarioLogado);

        Long quadro = boardService.create(quadroAberto("Sprint 12")).id();
        lista = listService.create(quadro, new BoardListRequest("Fazendo")).id();
    }

    @Test
    void cardComPrazoEmSeisHoras_geraCardDueSoonUmaVezSo() {
        cardComPrazo("Renovar domínio", OffsetDateTime.now().plusHours(6), false);

        scanner.varrer();
        scanner.varrer();

        assertThat(tiposGravados()).containsExactly(NotificationType.CARD_DUE_SOON);
    }

    @Test
    void cardComPrazoDaquiATresDias_naoEntraNaJanelaDeVinteEQuatroHoras() {
        cardComPrazo("Renovar domínio", OffsetDateTime.now().plusDays(3), false);

        scanner.varrer();

        assertThat(tiposGravados()).isEmpty();
    }

    @Test
    void cardVencidoENaoConcluido_geraCardDueOverdue() {
        cardComPrazo("Renovar domínio", OffsetDateTime.now().minusDays(1), false);

        scanner.varrer();

        assertThat(tiposGravados()).containsExactly(NotificationType.CARD_DUE_OVERDUE);
    }

    @Test
    void cardVencidoEConcluido_naoGeraNada() {
        cardComPrazo("Renovar domínio", OffsetDateTime.now().minusDays(1), true);

        scanner.varrer();

        assertThat(tiposGravados()).isEmpty();
    }

    @Test
    void cardSemResponsavel_naoGeraNadaPorqueNaoHaAQuemNotificar() {
        Long card = cardService.create(lista, new CardCreateRequest("Sem dono")).id();
        Card entidade = cardRepo.findById(card).orElseThrow();
        entidade.setDueDate(OffsetDateTime.now().plusHours(3));
        cardRepo.saveAndFlush(entidade);
        outboxRepo.deleteAll();

        scanner.varrer();

        assertThat(tiposGravados()).isEmpty();
    }

    private void cardComPrazo(String titulo, OffsetDateTime prazo, boolean concluido) {
        Long card = cardService.create(lista, new CardCreateRequest(titulo)).id();
        assigneeService.assign(card, colega.getId());

        Card entidade = cardRepo.findById(card).orElseThrow();
        entidade.setDueDate(prazo);
        entidade.setCompleted(concluido);
        cardRepo.saveAndFlush(entidade);

        outboxRepo.deleteAll();   // atribuir já notifica; aqui só interessam os prazos
    }

    private List<NotificationType> tiposGravados() {
        return outboxRepo.findBySentAtIsNullOrderByIdAsc(Limit.of(100)).stream()
                .map(linha -> mapper.readValue(linha.getPayload(), NotificationEvent.class))
                .map(NotificationEvent::type)
                .toList();
    }
}
