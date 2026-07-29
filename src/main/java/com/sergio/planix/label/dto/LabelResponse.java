package com.sergio.planix.label.dto;

import com.sergio.planix.label.Label;

import java.time.OffsetDateTime;

public record LabelResponse(
        Long id, Long boardId, String name, String color,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getBoard().getId(), label.getName(),
                label.getColor(), label.getCreatedAt(), label.getUpdatedAt());
    }
}
