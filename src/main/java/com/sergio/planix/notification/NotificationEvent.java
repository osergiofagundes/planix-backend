package com.sergio.planix.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        NotificationType type,
        Instant occurredAt,
        List<Long> recipients,
        Actor actor,
        Ref board,
        Ref card,
        Ref team,
        Map<String, String> data
) {

    public record Actor(Long id, String name, String avatarUrl) {}

    public record Ref(Long id, String name) {}

    public String kafkaKey() {
        return board != null ? "board:" + board.id() : "team:" + team.id();
    }
}
