package com.sergio.planix.attachment.dto;

import com.sergio.planix.attachment.Attachment;
import com.sergio.planix.auth.dto.UserSummary;

import java.time.OffsetDateTime;

public record AttachmentResponse(
        Long id, Long cardId, String originalFilename, String storedFilename,
        String contentType, Long sizeBytes, UserSummary author, OffsetDateTime createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getCard().getId(), a.getOriginalFilename(),
                a.getStoredFilename(), a.getContentType(), a.getSizeBytes(),
                UserSummary.from(a.getAuthor()), a.getCreatedAt());
    }
}
