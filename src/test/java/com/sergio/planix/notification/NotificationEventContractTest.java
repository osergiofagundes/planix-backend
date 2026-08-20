package com.sergio.planix.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O fixture é a rede de segurança da fronteira entre os dois repositórios: sem compilador
 * em comum, renomear um campo aqui só quebraria a desserialização lá, em execução. Este
 * teste e o gêmeo dele no planix-realtime trocam esse erro de runtime por um build vermelho.
 */
@JsonTest
class NotificationEventContractTest {

    @Autowired ObjectMapper mapper;

    @Test
    void serializarOEventoMontadoAMao_bateComOGoldenFixture() throws Exception {
        NotificationEvent evento = new NotificationEvent(
                UUID.fromString("9f1c2d3e-4a5b-4c7d-8e9f-0a1b2c3d4e5f"),
                NotificationType.CARD_MOVED,
                Instant.parse("2026-08-13T14:22:05Z"),
                List.of(12L, 15L, 31L),
                new NotificationEvent.Actor(7L, "Sérgio", "/api/users/7/avatar"),
                new NotificationEvent.Ref(4L, "Sprint 12"),
                new NotificationEvent.Ref(88L, "Ajustar filtro de cards"),
                null,
                Map.of("fromList", "Fazendo", "toList", "Revisão"));

        JsonNode esperado = mapper.readTree(
                new ClassPathResource("contract/notification-event.sample.json").getInputStream());

        // readTree dos dois lados: compara a ÁRVORE, não o texto — ordem de campo e espaço
        // em branco não devem quebrar o build.
        assertThat(mapper.readTree(mapper.writeValueAsString(evento))).isEqualTo(esperado);
    }

    @Test
    void eventoDeCard_usaAChaveDoQuadroNoKafka() {
        NotificationEvent evento = new NotificationEvent(
                UUID.randomUUID(), NotificationType.CARD_MOVED, Instant.now(), List.of(1L),
                null, new NotificationEvent.Ref(4L, "Sprint 12"),
                new NotificationEvent.Ref(88L, "Card"), null, Map.of());

        assertThat(evento.kafkaKey()).isEqualTo("board:4");
    }

    @Test
    void eventoDeEquipeSemQuadro_caiNoFallbackDaEquipeEmVezDeChaveNula() {
        NotificationEvent evento = new NotificationEvent(
                UUID.randomUUID(), NotificationType.TEAM_MEMBER_JOINED, Instant.now(), List.of(1L),
                null, null, null, new NotificationEvent.Ref(9L, "Equipe"), Map.of());

        assertThat(evento.kafkaKey()).isEqualTo("team:9");
    }
}
