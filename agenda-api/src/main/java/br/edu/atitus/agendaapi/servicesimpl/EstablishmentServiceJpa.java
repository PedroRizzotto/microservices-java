package br.edu.atitus.agendaapi.servicesimpl;

import br.edu.atitus.agendaapi.dtos.AvailabilityRequest;
import br.edu.atitus.agendaapi.dtos.EstablishmentRequest;
import br.edu.atitus.agendaapi.dtos.PetServiceRequest;
import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import br.edu.atitus.agendaapi.repositories.AvailabilityRepository;
import br.edu.atitus.agendaapi.repositories.EstablishmentRepository;
import br.edu.atitus.agendaapi.repositories.PetServiceRepository;
import br.edu.atitus.agendaapi.services.EstablishmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class EstablishmentServiceJpa implements EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final PetServiceRepository petServiceRepository;
    private final AvailabilityRepository availabilityRepository;

    public EstablishmentServiceJpa(EstablishmentRepository establishmentRepository,
                                   PetServiceRepository petServiceRepository,
                                   AvailabilityRepository availabilityRepository) {
        this.establishmentRepository = establishmentRepository;
        this.petServiceRepository = petServiceRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public EstablishmentEntity saveMine(Long providerId, EstablishmentRequest request) {
        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome informado inválido");

        EstablishmentEntity establishment = establishmentRepository.findByProviderId(providerId)
                .orElse(new EstablishmentEntity());
        establishment.setProviderId(providerId);
        establishment.setName(request.name());
        establishment.setAddress(request.address());
        establishment.setPhone(request.phone());
        return establishmentRepository.save(establishment);
    }

    @Override
    public EstablishmentEntity findMine(Long providerId) {
        return establishmentRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cadastre o estabelecimento primeiro"));
    }

    @Override
    public Page<EstablishmentEntity> findAll(Pageable pageable) {
        return establishmentRepository.findAll(pageable);
    }

    @Override
    public EstablishmentEntity getById(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));
    }

    @Override
    public PetServiceEntity addService(Long providerId, PetServiceRequest request) {
        EstablishmentEntity establishment = findMine(providerId);

        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome informado inválido");
        if (request.durationMinutes() == null || request.durationMinutes() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duração informada inválida");
        if (request.price() == null || request.price() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preço informado inválido");

        PetServiceEntity petService = new PetServiceEntity();
        petService.setEstablishmentId(establishment.getId());
        petService.setName(request.name());
        petService.setDurationMinutes(request.durationMinutes());
        petService.setPrice(request.price());
        petService.setActive(true);
        return petServiceRepository.save(petService);
    }

    @Override
    public void deactivateService(Long providerId, Long serviceId) {
        PetServiceEntity petService = petServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
        EstablishmentEntity establishment = establishmentRepository.findByProviderId(providerId).orElse(null);

        if (establishment == null || !establishment.getId().equals(petService.getEstablishmentId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");

        petService.setActive(false);
        petServiceRepository.save(petService);
    }

    @Override
    public List<PetServiceEntity> listActiveServices(Long establishmentId) {
        getById(establishmentId);
        return petServiceRepository.findByEstablishmentIdAndActiveTrue(establishmentId);
    }

    @Override
    @Transactional
    public List<AvailabilityEntity> replaceAvailability(Long providerId, List<AvailabilityRequest> request) {
        EstablishmentEntity establishment = findMine(providerId);

        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade informada inválida");

        List<AvailabilityEntity> availability = new ArrayList<>();
        for (AvailabilityRequest item : request) {
            if (item.dayOfWeek() == null || item.dayOfWeek() < 1 || item.dayOfWeek() > 7)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana inválido");
            if (item.startTime() == null || item.endTime() == null || !item.endTime().isAfter(item.startTime()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário informado inválido");

            AvailabilityEntity entity = new AvailabilityEntity();
            entity.setEstablishmentId(establishment.getId());
            entity.setDayOfWeek(item.dayOfWeek());
            entity.setStartTime(item.startTime());
            entity.setEndTime(item.endTime());
            availability.add(entity);
        }

        availabilityRepository.deleteByEstablishmentId(establishment.getId());
        return availabilityRepository.saveAll(availability);
    }

    @Override
    public List<AvailabilityEntity> listAvailability(Long establishmentId) {
        return availabilityRepository.findByEstablishmentId(establishmentId);
    }
}
