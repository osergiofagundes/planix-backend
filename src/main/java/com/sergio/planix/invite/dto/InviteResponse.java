package com.sergio.planix.invite.dto;

import com.sergio.planix.invite.BoardInvite;

import java.time.OffsetDateTime;

public record InviteResponse(Long id, int uses, int maxUses, OffsetDateTime expiresAt, OffsetDateTime revokedAt, OffsetDateTime createdAt) {

    public static InviteResponse from(BoardInvite invite) {
        return new InviteResponse(invite.getId(), invite.getUses(), invite.getMaxUses(),
                invite.getExpiresAt(), invite.getRevokedAt(), invite.getCreatedAt());
    }
}
