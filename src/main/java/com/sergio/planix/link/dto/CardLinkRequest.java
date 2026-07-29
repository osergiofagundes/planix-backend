package com.sergio.planix.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CardLinkRequest(
        @NotBlank @URL @Size(max = 2000) String url,
        @Size(max = 200) String title
) {}
