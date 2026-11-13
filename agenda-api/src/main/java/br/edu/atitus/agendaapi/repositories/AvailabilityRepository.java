package br.edu.atitus.agendaapi.repositories;

import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRepository extends JpaRepository<AvailabilityEntity, Long> {
    List<AvailabilityEntity> findByEstablishmentId(Long establishmentId);

    void deleteByEstablishmentId(Long establishmentId);
}
