package br.edu.atitus.agendaapi.services;

import br.edu.atitus.agendaapi.dtos.AvailabilityRequest;
import br.edu.atitus.agendaapi.dtos.EstablishmentRequest;
import br.edu.atitus.agendaapi.dtos.PetServiceRequest;
import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EstablishmentService {
    EstablishmentEntity saveMine(Long providerId, EstablishmentRequest request);

    EstablishmentEntity findMine(Long providerId);

    Page<EstablishmentEntity> findAll(Pageable pageable);

    EstablishmentEntity getById(Long id);

    PetServiceEntity addService(Long providerId, PetServiceRequest request);

    void deactivateService(Long providerId, Long serviceId);

    List<PetServiceEntity> listActiveServices(Long establishmentId);

    List<AvailabilityEntity> replaceAvailability(Long providerId, List<AvailabilityRequest> request);

    List<AvailabilityEntity> listAvailability(Long establishmentId);
}
