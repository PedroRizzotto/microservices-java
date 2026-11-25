package br.edu.atitus.authapi.dtos;

import br.edu.atitus.authapi.entities.AddressEntity;

public record AddressResponse(
        Long id,
        String label,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state) {

    public static AddressResponse from(AddressEntity entity) {
        return new AddressResponse(
                entity.getId(),
                entity.getLabel(),
                entity.getZipCode(),
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getDistrict(),
                entity.getCity(),
                entity.getState());
    }
}
