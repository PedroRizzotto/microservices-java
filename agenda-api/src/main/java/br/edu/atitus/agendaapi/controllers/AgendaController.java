package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import br.edu.atitus.agendaapi.services.AppointmentService;
import br.edu.atitus.agendaapi.services.EstablishmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agenda/establishments")
public class AgendaController {

    private final EstablishmentService establishmentService;
    private final AppointmentService appointmentService;

    public AgendaController(EstablishmentService establishmentService, AppointmentService appointmentService) {
        this.establishmentService = establishmentService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<Page<EstablishmentEntity>> getAll(
            @PageableDefault(size = 10, sort = "name", page = 0) Pageable pageable) {
        return ResponseEntity.ok(establishmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstablishmentEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(establishmentService.getById(id));
    }

    @GetMapping("/{id}/services")
    public ResponseEntity<List<PetServiceEntity>> getServices(@PathVariable Long id) {
        return ResponseEntity.ok(establishmentService.listActiveServices(id));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<String>> getSlots(
            @PathVariable Long id,
            @RequestParam Long serviceId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(appointmentService.freeSlots(id, serviceId, date));
    }
}
