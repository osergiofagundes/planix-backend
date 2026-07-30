package com.sergio.planix.invite.dto;

import java.time.OffsetDateTime;

public record InviteCreatedResponse(Long id, String token, OffsetDateTime expiresAt, int maxUses) {}
