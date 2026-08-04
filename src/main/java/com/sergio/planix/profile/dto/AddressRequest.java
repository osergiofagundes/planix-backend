package com.sergio.planix.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Endereço do usuário. Todo campo é opcional; mandar `null` limpa o que estava lá.")
public record AddressRequest(

        @Schema(description = "Logradouro", example = "Rua das Acácias")
        @Size(max = 150) String street,

        @Schema(description = "Número", example = "1024")
        @Size(max = 20) String number,

        @Schema(description = "Complemento", example = "Apto 32B")
        @Size(max = 100) String complement,

        @Schema(description = "Cidade", example = "Porto Alegre")
        @Size(max = 100) String city,

        @Schema(description = "Sigla do estado. Guardada sempre em maiúsculas.", example = "RS")
        @Pattern(regexp = "^([A-Za-z]{2})?$", message = "deve ser a sigla do estado, com 2 letras")
        String state,

        @Schema(description = "CEP, com ou sem hífen", example = "90000-000")
        @Pattern(regexp = "^(\\d{5}-?\\d{3})?$", message = "deve ter 8 dígitos, com ou sem hífen")
        String zipCode
) {}
