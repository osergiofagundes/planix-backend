package com.sergio.planix.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Quem passa a ser o dono do quadro.")
public record OwnerTransferRequest(
        @Schema(description = "Id do novo dono. Precisa já ser membro do quadro.", example = "7")
        @NotNull Long userId
) {}
