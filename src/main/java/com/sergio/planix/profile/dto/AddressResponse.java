package com.sergio.planix.profile.dto;

import com.sergio.planix.auth.Address;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Endereço do usuário. Vem `null` quando nada foi preenchido.")
public record AddressResponse(
        @Schema(example = "Rua das Acácias") String street,
        @Schema(example = "1024") String number,
        @Schema(example = "Apto 32B") String complement,
        @Schema(example = "Porto Alegre") String city,
        @Schema(example = "RS") String state,
        @Schema(example = "90000-000") String zipCode
) {

    public static AddressResponse from(Address address) {
        return address == null ? null : new AddressResponse(
                address.getStreet(), address.getNumber(), address.getComplement(),
                address.getCity(), address.getState(), address.getZipCode());
    }
}
