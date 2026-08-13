package com.sergio.planix.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "O emoji de uma reação. Quem reage sai do token — não se manda aqui.")
public record CommentReactionRequest(
        @Schema(description = "Emoji da reação", example = "👍")
        @NotBlank @Size(max = 32) String emoji
) {}
