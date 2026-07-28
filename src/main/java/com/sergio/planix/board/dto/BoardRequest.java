package com.sergio.planix.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description
) {}