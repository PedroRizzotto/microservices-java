package br.edu.atitus.orderapi.dtos;

public record AddressResponse(
        Long id,
        String label,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state
) {}
