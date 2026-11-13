package br.edu.atitus.agendaapi.services;

import br.edu.atitus.agendaapi.dtos.AppointmentRequest;
import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentEntity book(Long customerId, String customerEmail, AppointmentRequest request);

    Page<AppointmentEntity> findMine(Long customerId, Pageable pageable);

    Page<AppointmentEntity> findReceived(Long providerId, Pageable pageable);

    AppointmentEntity cancel(Long appointmentId, Long userId);

    List<String> freeSlots(Long establishmentId, Long serviceId, LocalDate date);
}
