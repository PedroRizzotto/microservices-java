package br.edu.atitus.agendaapi.repositories;

import br.edu.atitus.agendaapi.entities.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<PetEntity, Long> {
    List<PetEntity> findByCustomerIdAndActiveTrueOrderByNameAsc(Long customerId);

    Optional<PetEntity> findByIdAndCustomerIdAndActiveTrue(Long id, Long customerId);
}
