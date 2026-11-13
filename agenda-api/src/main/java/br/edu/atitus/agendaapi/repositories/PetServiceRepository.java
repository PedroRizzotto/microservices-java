package br.edu.atitus.agendaapi.repositories;

import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetServiceRepository extends JpaRepository<PetServiceEntity, Long> {
    List<PetServiceEntity> findByEstablishmentIdAndActiveTrue(Long establishmentId);
}
