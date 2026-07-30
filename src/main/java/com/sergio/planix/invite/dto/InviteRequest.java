package com.sergio.planix.invite.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record InviteRequest(
        @Min(1) @Max(30) Integer expiresInDays,
        @Min(1) @Max(50) Integer maxUses) {}
