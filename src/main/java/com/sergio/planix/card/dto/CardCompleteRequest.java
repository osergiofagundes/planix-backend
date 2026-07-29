package com.sergio.planix.card.dto;

import jakarta.validation.constraints.NotNull;

public record CardCompleteRequest(
        @NotNull Boolean completed
) {}
