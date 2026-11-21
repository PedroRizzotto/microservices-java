package br.edu.atitus.authapi.dtos;

public record AddressRequest(String label, String zipCode, String street, String number,
                             String complement, String district, String city, String state) {
}
