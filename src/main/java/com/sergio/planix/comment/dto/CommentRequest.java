package com.sergio.planix.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "O texto de um comentário. O autor sai do token — não se manda aqui.")
public record CommentRequest(
        @Schema(description = "Conteúdo do comentário",
                example = "O registro.br cobra por 2 anos no mínimo, dá R$ 80.")
        @NotBlank @Size(max = 5000) String text,

        @Schema(description = """
                Comentário que está sendo respondido. `null` cria um comentário raiz. \
                Responder a uma resposta é aceito — o comentário novo é reparentado \
                para a raiz da thread, porque a árvore só tem dois níveis.""",
                example = "300")
        Long parentId
) {}
