package com.sergio.planix.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "planix.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int TIMEOUT_DE_ENVIO_EM_SEGUNDOS = 5;

    private final NotificationOutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topico;
    private final int lote;
    private final int diasDeRetencao;

    public OutboxRelay(NotificationOutboxRepository repo, KafkaTemplate<String, String> kafka,
                       ObjectMapper mapper,
                       @Value("${planix.notifications.topic}") String topico,
                       @Value("${planix.outbox.relay.batch:100}") int lote,
                       @Value("${planix.outbox.retention-days:7}") int diasDeRetencao) {
        this.repo = repo;
        this.kafka = kafka;
        this.mapper = mapper;
        this.topico = topico;
        this.lote = lote;
        this.diasDeRetencao = diasDeRetencao;
    }

    @Scheduled(fixedDelayString = "${planix.outbox.relay.interval:2s}")
    @Transactional
    public int drenar() {
        List<NotificationOutbox> pendentes = repo.findBySentAtIsNullOrderByIdAsc(Limit.of(lote));
        int publicadas = 0;

        for (NotificationOutbox linha : pendentes) {
            try {
                kafka.send(topico, chaveDe(linha), linha.getPayload())
                        .get(TIMEOUT_DE_ENVIO_EM_SEGUNDOS, TimeUnit.SECONDS);

                linha.setSentAt(OffsetDateTime.now());
                publicadas++;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.warn("Falha ao publicar a linha {} da outbox; a fila drena na próxima passada",
                        linha.getId(), ex);
                break;
            }
        }
        return publicadas;
    }

    @Scheduled(cron = "${planix.outbox.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int limpar() {
        return repo.deleteBySentAtBefore(OffsetDateTime.now().minusDays(diasDeRetencao));
    }

    private String chaveDe(NotificationOutbox linha) {
        return mapper.readValue(linha.getPayload(), NotificationEvent.class).kafkaKey();
    }
}
