package com.sergio.planix.checklist.dto;

import com.sergio.planix.checklist.ChecklistItem;

import java.time.OffsetDateTime;

public record ChecklistItemResponse(
        Long id, Long cardId, String text, boolean done, int position,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(item.getId(), item.getCard().getId(), item.getText(),
                item.isDone(), item.getPosition(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
