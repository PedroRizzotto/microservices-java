package br.edu.atitus.agendaapi.repositories;

import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstablishmentRepository extends JpaRepository<EstablishmentEntity, Long> {
    Optional<EstablishmentEntity> findByProviderId(Long providerId);
}
