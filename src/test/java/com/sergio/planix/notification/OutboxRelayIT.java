package com.sergio.planix.notification;

import com.sergio.planix.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/*
 * Único IT que sobe com o relay ligado. O KafkaTemplate é mockado de propósito: o relay é
 * lógica (ordem, marcação de enviada, parada no erro) e o broker é infra — a suíte do planix
 * continua subindo só o Postgres, que é justamente o que a outbox comprou.
 *
 * Transacional: cada teste rola atrás no fim, sem sujar a outbox para os demais ITs.
 */
@Transactional
@TestPropertySource(properties = "planix.outbox.relay.enabled=true")
class OutboxRelayIT extends IntegrationTest {

    @Autowired OutboxRelay relay;
    @Autowired NotificationOutboxRepository repo;
    @Autowired ObjectMapper mapper;

    @MockitoBean KafkaTemplate<String, String> kafka;

    @Test
    void drenar_publicaAsPendentesEAsMarcaComoEnviadas() {
        enviaComSucesso();
        repo.deleteAll();
        gravar(NotificationType.CARD_MOVED, 4L);
        gravar(NotificationType.CARD_ASSIGNED, 4L);

        assertThat(relay.drenar()).isEqualTo(2);
        assertThat(pendentes()).isEmpty();
    }

    @Test
    void drenar_usaAChaveDoQuadroParaAgruparNaMesmaParticao() {
        enviaComSucesso();
        repo.deleteAll();
        gravar(NotificationType.CARD_MOVED, 4L);

        relay.drenar();

        ArgumentCaptor<String> chave = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(kafka).send(anyString(), chave.capture(), anyString());
        assertThat(chave.getValue()).isEqualTo("board:4");
    }

    @Test
    void drenarComAFilaVazia_naoFazNadaEDevolveZero() {
        enviaComSucesso();
        repo.deleteAll();

        assertThat(relay.drenar()).isZero();
    }

    @Test
    void kafkaForaDoAr_naoMarcaComoEnviadaEAFilaAcumula() {
        when(kafka.send(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker fora")));
        repo.deleteAll();
        gravar(NotificationType.CARD_MOVED, 4L);

        assertThat(relay.drenar()).isZero();
        assertThat(pendentes()).hasSize(1);
    }

    @Test
    void limpar_apagaAsEnviadasComMaisDeSeteDiasEPreservaAsPendentes() {
        repo.deleteAll();
        UUID velha = gravar(NotificationType.CARD_MOVED, 4L);
        gravar(NotificationType.CARD_ASSIGNED, 4L);

        NotificationOutbox antiga = pendentes().stream()
                .filter(linha -> linha.getEventId().equals(velha)).findFirst().orElseThrow();
        antiga.setSentAt(OffsetDateTime.now().minusDays(8));
        repo.saveAndFlush(antiga);

        assertThat(relay.limpar()).isEqualTo(1);
        assertThat(pendentes()).hasSize(1);
    }

    private void enviaComSucesso() {
        when(kafka.send(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));
    }

    private UUID gravar(NotificationType tipo, Long boardId) {
        UUID eventId = UUID.randomUUID();
        NotificationEvent evento = new NotificationEvent(eventId, tipo, Instant.now(),
                List.of(12L), new NotificationEvent.Actor(7L, "Sérgio", null),
                new NotificationEvent.Ref(boardId, "Sprint 12"),
                new NotificationEvent.Ref(88L, "Ajustar filtro"), null, Map.of());

        repo.inserirIgnorandoDuplicado(eventId, tipo.name(), mapper.writeValueAsString(evento));
        return eventId;
    }

    private List<NotificationOutbox> pendentes() {
        return repo.findBySentAtIsNullOrderByIdAsc(Limit.of(100));
    }
}
