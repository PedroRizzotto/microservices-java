package br.edu.atitus.agendaapi.servicesimpl;

import br.edu.atitus.agendaapi.components.AgendaRules;
import br.edu.atitus.agendaapi.dtos.AppointmentRequest;
import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.entities.AppointmentStatus;
import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import br.edu.atitus.agendaapi.repositories.AppointmentRepository;
import br.edu.atitus.agendaapi.repositories.AvailabilityRepository;
import br.edu.atitus.agendaapi.repositories.EstablishmentRepository;
import br.edu.atitus.agendaapi.repositories.PetServiceRepository;
import br.edu.atitus.agendaapi.services.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentServiceJpa implements AppointmentService {

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final EstablishmentRepository establishmentRepository;
    private final PetServiceRepository petServiceRepository;
    private final AvailabilityRepository availabilityRepository;

    public AppointmentServiceJpa(AppointmentRepository appointmentRepository,
                                 EstablishmentRepository establishmentRepository,
                                 PetServiceRepository petServiceRepository,
                                 AvailabilityRepository availabilityRepository) {
        this.appointmentRepository = appointmentRepository;
        this.establishmentRepository = establishmentRepository;
        this.petServiceRepository = petServiceRepository;
        this.availabilityRepository = availabilityRepository;
    }

    private EstablishmentEntity getEstablishment(Long establishmentId) {
        if (establishmentId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estabelecimento não informado");
        return establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));
    }

    private PetServiceEntity getActiveService(Long serviceId, Long establishmentId) {
        PetServiceEntity petService = serviceId == null ? null : petServiceRepository.findById(serviceId).orElse(null);
        if (petService == null || !petService.isActive() || !petService.getEstablishmentId().equals(establishmentId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serviço indisponível para este estabelecimento");
        return petService;
    }

    private List<AppointmentEntity> findBooked(Long establishmentId, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByEstablishmentIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                establishmentId, AppointmentStatus.AGENDADO, end, start);
    }

    @Override
    public AppointmentEntity book(Long customerId, String customerEmail, AppointmentRequest request) {
        EstablishmentEntity establishment = getEstablishment(request.establishmentId());
        PetServiceEntity petService = getActiveService(request.serviceId(), establishment.getId());

        if (request.petName() == null || request.petName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do pet informado inválido");
        if (request.startAt() == null || request.startAt().isBefore(AgendaRules.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e hora informadas inválidas");

        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = startAt.plusMinutes(petService.getDurationMinutes());

        List<AvailabilityEntity> windows = availabilityRepository.findByEstablishmentId(establishment.getId());
        if (!AgendaRules.fitsInAvailability(startAt, endAt, windows))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário fora do atendimento do pet shop");

        if (!findBooked(establishment.getId(), startAt, endAt).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Horário já ocupado");

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setEstablishmentId(establishment.getId());
        appointment.setPetService(petService);
        appointment.setCustomerId(customerId);
        appointment.setCustomerEmail(customerEmail);
        appointment.setPetName(request.petName());
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setPriceAtBooking(petService.getPrice());
        appointment.setStatus(AppointmentStatus.AGENDADO);
        return appointmentRepository.save(appointment);
    }

    @Override
    public Page<AppointmentEntity> findMine(Long customerId, Pageable pageable) {
        return appointmentRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    public Page<AppointmentEntity> findReceived(Long providerId, Pageable pageable) {
        EstablishmentEntity establishment = establishmentRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cadastre o estabelecimento primeiro"));
        return appointmentRepository.findByEstablishmentId(establishment.getId(), pageable);
    }

    @Override
    public AppointmentEntity cancel(Long appointmentId, Long userId) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));
        EstablishmentEntity establishment = getEstablishment(appointment.getEstablishmentId());

        if (!appointment.getCustomerId().equals(userId) && !establishment.getProviderId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");
        if (appointment.getStatus() == AppointmentStatus.CANCELADO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agendamento já cancelado");

        appointment.setStatus(AppointmentStatus.CANCELADO);
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<String> freeSlots(Long establishmentId, Long serviceId, LocalDate date) {
        EstablishmentEntity establishment = getEstablishment(establishmentId);
        PetServiceEntity petService = getActiveService(serviceId, establishment.getId());

        List<AvailabilityEntity> windows = availabilityRepository.findByEstablishmentId(establishment.getId());
        List<LocalDateTime[]> busy = findBooked(establishment.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .map(appointment -> new LocalDateTime[]{appointment.getStartAt(), appointment.getEndAt()})
                .toList();

        List<LocalTime> slots = AgendaRules.freeSlots(date, petService.getDurationMinutes(), windows, busy, AgendaRules.now());
        return slots.stream().map(HOUR_FORMAT::format).toList();
    }
}
