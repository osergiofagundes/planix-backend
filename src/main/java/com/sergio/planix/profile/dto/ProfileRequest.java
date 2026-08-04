package com.sergio.planix.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = """
        Dados do perfil. É um PUT: o que vier `null` (ou for omitido) **apaga** o valor guardado.
        Só o nome é obrigatório. E-mail e senha não se trocam por aqui.""")
public record ProfileRequest(

        @Schema(description = "Nome de exibição", example = "Sérgio Fagundes")
        @NotBlank @Size(max = 150) String name,

        @Schema(description = "Data de nascimento, no formato ISO", example = "1990-04-17")
        @Past(message = "deve ser uma data no passado") LocalDate birthDate,

        @Schema(description = "Telefone com DDD", example = "+55 51 99999-0000")
        @Pattern(regexp = "^(\\+?[0-9 ()-]{8,20})?$", message = "telefone inválido")
        String phone,

        @Schema(description = "Texto livre sobre você", example = "Backend em Java, aprendendo Spring.")
        @Size(max = 1000) String bio,

        @Schema(description = "Endereço completo. Opcional.")
        @Valid AddressRequest address
) {}
