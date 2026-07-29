package com.sergio.planix.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardListRequest(
        @NotBlank @Size(max = 150) String name
) {}
