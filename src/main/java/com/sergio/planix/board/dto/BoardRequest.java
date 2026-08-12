package com.sergio.planix.board.dto;

import com.sergio.planix.board.BoardVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para renomear, redescrever ou mudar a visibilidade de um quadro.")
public record BoardRequest(
        @Schema(description = "Nome do quadro", example = "Lançamento do site")
        @NotBlank @Size(max = 150) String name,

        @Schema(description = "Descrição livre. Opcional.",
                example = "Tudo que precisa sair antes de colocar o site no ar.")
        @Size(max = 2000) String description,

        @Schema(description = "Chave do ícone do quadro. Opcional.", example = "rocket")
        @Size(max = 50) String icon,

        @Schema(description = "Quem da equipe enxerga o quadro. Omitido, vira `TEAM`.",
                example = "TEAM")
        BoardVisibility visibility
) {
    public BoardRequest(String name, String description) {
        this(name, description, null, null);
    }

    public BoardVisibility visibilityOrDefault() {
        return visibility == null ? BoardVisibility.TEAM : visibility;
    }
}
