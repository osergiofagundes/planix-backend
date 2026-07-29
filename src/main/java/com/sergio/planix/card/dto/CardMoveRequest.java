package com.sergio.planix.card.dto;

import jakarta.validation.constraints.NotNull;

public record CardMoveRequest(
        @NotNull Long targetListId,
        @NotNull Integer position
) {}
