package br.edu.atitus.agendaapi.dtos;

public record PetServiceRequest(
        String name,
        Integer durationMinutes,
        Double price
) {
}
