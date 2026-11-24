package br.edu.atitus.agendaapi.services;

import br.edu.atitus.agendaapi.dtos.AppointmentRequest;
import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentEntity book(Long customerId, String customerEmail, AppointmentRequest request);

    AppointmentEntity getOne(Long appointmentId, Long userId);

    Page<AppointmentEntity> findMine(Long customerId, Pageable pageable);

    Page<AppointmentEntity> findReceived(Long providerId, Pageable pageable);

    AppointmentEntity cancel(Long appointmentId, Long userId);

    AppointmentEntity pay(Long customerId, Long appointmentId, String method);

    AppointmentEntity complete(Long providerId, Long appointmentId);

    List<String> freeSlots(Long establishmentId, Long serviceId, LocalDate date);
}
