package com.sergio.planix.notification;

import com.sergio.planix.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class NotificationOutboxIT extends IntegrationTest {

    @Autowired NotificationOutboxRepository repo;

    @Test
    void inserirDuasVezesOMesmoEventId_gravaUmaLinhaSoENaoEstoura() {
        UUID eventId = UUID.randomUUID();

        assertThat(repo.inserirIgnorandoDuplicado(eventId, "CARD_MOVED", "{\"a\":1}")).isEqualTo(1);
        assertThat(repo.inserirIgnorandoDuplicado(eventId, "CARD_MOVED", "{\"a\":2}")).isZero();

        List<NotificationOutbox> pendentes = pendentesDe(eventId);
        assertThat(pendentes).hasSize(1);
        assertThat(pendentes.getFirst().getType()).isEqualTo("CARD_MOVED");
        assertThat(pendentes.getFirst().getPayload()).contains("\"a\": 1");
    }

    @Test
    void marcarComoEnviada_tiraALinhaDaFilaDePendentes() {
        UUID eventId = UUID.randomUUID();
        repo.inserirIgnorandoDuplicado(eventId, "CARD_ASSIGNED", "{}");

        NotificationOutbox linha = pendentesDe(eventId).getFirst();
        linha.setSentAt(OffsetDateTime.now());
        repo.saveAndFlush(linha);

        assertThat(pendentesDe(eventId)).isEmpty();
    }

    @Test
    void limparEnviadasAntigas_apagaAsVelhasEPreservaAsPendentes() {
        UUID antiga = UUID.randomUUID();
        UUID pendente = UUID.randomUUID();
        repo.inserirIgnorandoDuplicado(antiga, "CARD_MOVED", "{}");
        repo.inserirIgnorandoDuplicado(pendente, "CARD_MOVED", "{}");

        NotificationOutbox linha = pendentesDe(antiga).getFirst();
        linha.setSentAt(OffsetDateTime.now().minusDays(8));
        repo.saveAndFlush(linha);

        assertThat(repo.deleteBySentAtBefore(OffsetDateTime.now().minusDays(7))).isEqualTo(1);
        assertThat(pendentesDe(pendente)).hasSize(1);
    }

    private List<NotificationOutbox> pendentesDe(UUID eventId) {
        return repo.findBySentAtIsNullOrderByIdAsc(Limit.of(100)).stream()
                .filter(linha -> linha.getEventId().equals(eventId))
                .toList();
    }
}
