package br.edu.atitus.agendaapi.dtos;

import java.time.LocalDateTime;

public record AppointmentRequest(
        Long establishmentId,
        Long serviceId,
        LocalDateTime startAt,
        String petName
) {
}
