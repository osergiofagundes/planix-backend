package com.sergio.planix.card.dto;

import com.sergio.planix.card.Card;
import com.sergio.planix.card.Priority;
import com.sergio.planix.label.dto.LabelResponse;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

public record CardResponse(
        Long id, Long listId, String title, String description,
        OffsetDateTime dueDate, Priority priority, int position,
        boolean completed, OffsetDateTime completedAt,
        List<LabelResponse> labels,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CardResponse from(Card card) {
        List<LabelResponse> labels = card.getLabels().stream()
                .map(LabelResponse::from)
                .sorted(Comparator.comparing(LabelResponse::name))
                .toList();
        return new CardResponse(card.getId(), card.getList().getId(), card.getTitle(),
                card.getDescription(), card.getDueDate(), card.getPriority(), card.getPosition(),
                card.isCompleted(), card.getCompletedAt(), labels,
                card.getCreatedAt(), card.getUpdatedAt());
    }
}
