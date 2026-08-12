package com.sergio.planix.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Quem passa a ter acesso ao quadro.")
public record AddMemberRequest(
        @Schema(description = "Id do usuário. Precisa já ser membro da equipe do quadro.", example = "7")
        @NotNull Long userId
) {}
