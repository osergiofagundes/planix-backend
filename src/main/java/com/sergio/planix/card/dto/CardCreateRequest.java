package com.sergio.planix.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardCreateRequest(
        @NotBlank @Size(max = 200) String title
) {}
