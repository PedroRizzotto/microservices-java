package br.edu.atitus.agendaapi.dtos;

import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        Long establishmentId,
        Integer dayOfWeek,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime) {

    public static AvailabilityResponse from(AvailabilityEntity entity) {
        return new AvailabilityResponse(
                entity.getId(),
                entity.getEstablishmentId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime());
    }
}
