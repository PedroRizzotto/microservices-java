package br.edu.atitus.agendaapi.dtos;

import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.entities.AppointmentStatus;
import br.edu.atitus.agendaapi.entities.PetSpecies;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long establishmentId,
        PetServiceResponse petService,
        String customerEmail,
        Long petId,
        String petName,
        PetSpecies petSpecies,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endAt,
        double priceAtBooking,
        AppointmentStatus status,
        Long paymentId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime paidAt) {

    public static AppointmentResponse from(AppointmentEntity entity) {
        return new AppointmentResponse(
                entity.getId(),
                entity.getEstablishmentId(),
                PetServiceResponse.from(entity.getPetService()),
                entity.getCustomerEmail(),
                entity.getPetId(),
                entity.getPetName(),
                entity.getPetSpecies(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getPriceAtBooking(),
                entity.getStatus(),
                entity.getPaymentId(),
                entity.getPaidAt());
    }
}
