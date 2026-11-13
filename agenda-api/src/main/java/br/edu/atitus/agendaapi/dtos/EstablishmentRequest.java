package br.edu.atitus.agendaapi.dtos;

public record EstablishmentRequest(
        String name,
        String address,
        String phone
) {
}
