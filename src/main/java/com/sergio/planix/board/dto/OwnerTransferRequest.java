package com.sergio.planix.board.dto;

import jakarta.validation.constraints.NotNull;

public record OwnerTransferRequest(@NotNull Long userId) {}
