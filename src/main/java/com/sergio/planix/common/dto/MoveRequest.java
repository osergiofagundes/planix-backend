package com.sergio.planix.common.dto;

import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull Integer position
) {}
