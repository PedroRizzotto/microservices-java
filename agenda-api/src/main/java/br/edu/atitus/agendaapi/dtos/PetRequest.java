package br.edu.atitus.agendaapi.dtos;

import java.time.LocalDate;

public record PetRequest(
        String name,
        String species,
        String breed,
        LocalDate birthDate,
        Double weight,
        String notes
) {
}
