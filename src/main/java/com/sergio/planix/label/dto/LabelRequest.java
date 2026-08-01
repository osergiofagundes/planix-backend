package com.sergio.planix.label.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de uma etiqueta do quadro.")
public record LabelRequest(
        @Schema(description = "Nome da etiqueta. Único dentro do quadro.", example = "Urgente")
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "Cor, como o cliente quiser representá-la. A API guarda a string "
                            + "como veio — não valida formato.",
                example = "#E53935")
        @NotBlank @Size(max = 30) String color
) {}
