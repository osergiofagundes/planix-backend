package com.sergio.planix.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "O par de tokens devolvido pelo cadastro, pelo login e pelo refresh.")
public record AuthResponse(
        @Schema(description = "JWT assinado. Vai em `Authorization: Bearer <token>` nas demais chamadas.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZXhwIjoxNzUzODAwMDAwfQ.4pcPyMD09olPSyXnrXCjTw")
        String accessToken,

        @Schema(description = "String opaca de vida longa, usada só em `/api/auth/refresh`. "
                            + "Diferente do access token, este pode ser revogado.",
                example = "9f2b7c1e4a8d0356b1f7e2c9d4a60b83f5172e9cd8a34b6027f1e5c9d3a80b64")
        String refreshToken,

        @Schema(description = "Validade do access token, em segundos", example = "900")
        long expiresInSeconds
) {}
