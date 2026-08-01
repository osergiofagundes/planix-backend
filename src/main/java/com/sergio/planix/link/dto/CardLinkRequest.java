package com.sergio.planix.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Um link externo para anexar ao cartão. Nada é baixado do endereço.")
public record CardLinkRequest(
        @Schema(description = "Endereço completo, com esquema", example = "https://registro.br/dominio/")
        @NotBlank @URL @Size(max = 2000) String url,

        @Schema(description = "Rótulo para exibir no lugar da URL. Opcional.",
                example = "Consulta de domínios .br")
        @Size(max = 200) String title
) {}
