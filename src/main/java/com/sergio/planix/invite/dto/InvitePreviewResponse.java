package com.sergio.planix.invite.dto;

import com.sergio.planix.auth.dto.UserSummary;

import java.time.OffsetDateTime;

public record InvitePreviewResponse(String boardName, UserSummary invitedBy, OffsetDateTime expiresAt) {}
