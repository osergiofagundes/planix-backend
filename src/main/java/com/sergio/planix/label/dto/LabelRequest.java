package com.sergio.planix.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String color
) {}
