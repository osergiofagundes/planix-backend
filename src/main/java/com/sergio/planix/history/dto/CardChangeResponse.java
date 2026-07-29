package com.sergio.planix.history.dto;

import com.sergio.planix.history.CardChange;

import java.time.OffsetDateTime;

public record CardChangeResponse(
        String field, String oldValue, String newValue, OffsetDateTime changedAt
) {
    public static CardChangeResponse from(CardChange change) {
        return new CardChangeResponse(change.getField(), change.getOldValue(),
                change.getNewValue(), change.getChangedAt());
    }
}
