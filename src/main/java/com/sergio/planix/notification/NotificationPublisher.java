package com.sergio.planix.notification;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.dto.AvatarUrl;
import com.sergio.planix.board.Board;
import com.sergio.planix.card.Card;
import com.sergio.planix.team.Team;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationPublisher {

    private static final int TAMANHO_TRECHO = 120;

    private final NotificationOutboxRepository outboxRepo;
    private final RecipientResolver recipients;
    private final CurrentUser currentUser;
    private final ObjectMapper mapper;

    public NotificationPublisher(NotificationOutboxRepository outboxRepo,
                                 RecipientResolver recipients, CurrentUser currentUser,
                                 ObjectMapper mapper) {
        this.outboxRepo = outboxRepo;
        this.recipients = recipients;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    public void cardMoved(Card card, String fromList, String toList) {
        NotificationEvent.Actor ator = atorAtual();
        publish(deCard(UUID.randomUUID(), NotificationType.CARD_MOVED, card, ator,
                recipients.doQuadro(boardDe(card).getId(), ator.id()),
                Map.of("fromList", fromList, "toList", toList)));
    }

    public void cardAssigned(Card card, Long assigneeId) {
        NotificationEvent.Actor ator = atorAtual();
        publish(deCard(UUID.randomUUID(), NotificationType.CARD_ASSIGNED, card, ator,
                assigneeId.equals(ator.id()) ? List.of() : List.of(assigneeId),
                Map.of()));
    }

    public void cardCommented(Card card, String texto) {
        NotificationEvent.Actor ator = atorAtual();
        publish(deCard(UUID.randomUUID(), NotificationType.CARD_COMMENTED, card, ator,
                recipients.responsaveisDoCard(card.getId(), ator.id()),
                Map.of("excerpt", trecho(texto))));
    }

    public void cardDueSoon(Card card) {
        prazo(NotificationType.CARD_DUE_SOON, card);
    }

    public void cardDueOverdue(Card card) {
        prazo(NotificationType.CARD_DUE_OVERDUE, card);
    }

    public void teamMemberJoined(Team team) {
        NotificationEvent.Actor ator = atorAtual();
        publish(new NotificationEvent(UUID.randomUUID(), NotificationType.TEAM_MEMBER_JOINED,
                Instant.now(), recipients.gestoresDaEquipe(team.getId(), ator.id()),
                ator, null, null, new NotificationEvent.Ref(team.getId(), team.getName()),
                Map.of()));
    }

    public void publish(NotificationEvent evento) {
        if (evento.recipients().isEmpty()) {
            return;
        }
        outboxRepo.inserirIgnorandoDuplicado(
                evento.eventId(), evento.type().name(), mapper.writeValueAsString(evento));
    }

    private void prazo(NotificationType tipo, Card card) {
        publish(deCard(idDeterminado(tipo, card), tipo, card, null,
                recipients.responsaveisDoCard(card.getId(), null),
                Map.of("dueDate", card.getDueDate().toInstant().toString())));
    }

    private NotificationEvent deCard(UUID eventId, NotificationType tipo, Card card,
                                     NotificationEvent.Actor ator, List<Long> destinatarios,
                                     Map<String, String> data) {
        Board board = boardDe(card);
        return new NotificationEvent(eventId, tipo, Instant.now(), destinatarios, ator,
                new NotificationEvent.Ref(board.getId(), board.getName()),
                new NotificationEvent.Ref(card.getId(), card.getTitle()),
                null, data);
    }

    private static UUID idDeterminado(NotificationType tipo, Card card) {
        String semente = "%s:%d:%d".formatted(
                tipo.name(), card.getId(), card.getDueDate().toInstant().toEpochMilli());
        return UUID.nameUUIDFromBytes(semente.getBytes(StandardCharsets.UTF_8));
    }

    private NotificationEvent.Actor atorAtual() {
        User user = currentUser.reference();
        return new NotificationEvent.Actor(user.getId(), user.getName(), AvatarUrl.of(user));
    }

    private static Board boardDe(Card card) {
        return card.getList().getBoard();
    }

    private static String trecho(String texto) {
        String limpo = texto == null ? "" : texto.strip();
        return limpo.length() <= TAMANHO_TRECHO ? limpo
                : limpo.substring(0, TAMANHO_TRECHO).stripTrailing() + "…";
    }
}
