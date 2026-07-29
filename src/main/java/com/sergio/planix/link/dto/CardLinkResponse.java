package com.sergio.planix.link.dto;

import com.sergio.planix.link.CardLink;

import java.time.OffsetDateTime;

public record CardLinkResponse(
        Long id, Long cardId, String url, String title,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CardLinkResponse from(CardLink link) {
        return new CardLinkResponse(link.getId(), link.getCard().getId(), link.getUrl(),
                link.getTitle(), link.getCreatedAt(), link.getUpdatedAt());
    }
}
