package com.sergio.planix.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "O texto de um comentário. O autor sai do token — não se manda aqui.")
public record CommentRequest(
        @Schema(description = "Conteúdo do comentário",
                example = "O registro.br cobra por 2 anos no mínimo, dá R$ 80.")
        @NotBlank @Size(max = 5000) String text
) {}
