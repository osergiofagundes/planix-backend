package com.sergio.planix.card.dto;

import com.sergio.planix.card.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CardUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        OffsetDateTime dueDate,
        Priority priority
) {}
