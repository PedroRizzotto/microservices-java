package br.edu.atitus.agendaapi.repositories;

import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.entities.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {
    Page<AppointmentEntity> findByCustomerId(Long customerId, Pageable pageable);

    Page<AppointmentEntity> findByEstablishmentId(Long establishmentId, Pageable pageable);

    List<AppointmentEntity> findByEstablishmentIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
            Long establishmentId, AppointmentStatus status, LocalDateTime end, LocalDateTime start);
}
