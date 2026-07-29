package com.sergio.planix.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank @Size(max = 5000) String text
) {}
