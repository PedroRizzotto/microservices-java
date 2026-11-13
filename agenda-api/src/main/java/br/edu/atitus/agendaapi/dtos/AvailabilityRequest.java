package br.edu.atitus.agendaapi.dtos;

import java.time.LocalTime;

public record AvailabilityRequest(
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
