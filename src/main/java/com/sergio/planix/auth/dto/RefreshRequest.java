package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "O refresh token recebido no login.")
public record RefreshRequest(
        @Schema(description = "Refresh token opaco — não é um JWT, é uma string aleatória guardada no banco",
                example = "9f2b7c1e4a8d0356b1f7e2c9d4a60b83f5172e9cd8a34b6027f1e5c9d3a80b64")
        @NotBlank String refreshToken
) {}
