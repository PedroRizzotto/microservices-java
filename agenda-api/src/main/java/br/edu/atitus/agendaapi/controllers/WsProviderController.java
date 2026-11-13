package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.AvailabilityRequest;
import br.edu.atitus.agendaapi.dtos.EstablishmentRequest;
import br.edu.atitus.agendaapi.dtos.PetServiceRequest;
import br.edu.atitus.agendaapi.entities.AvailabilityEntity;
import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.entities.PetServiceEntity;
import br.edu.atitus.agendaapi.services.EstablishmentService;
import br.edu.atitus.agendaapi.servicesimpl.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/ws/agenda")
public class WsProviderController {

    private final EstablishmentService service;
    private final AuthorizationService authService;

    public WsProviderController(EstablishmentService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PutMapping("/establishment")
    public ResponseEntity<EstablishmentEntity> putEstablishment(
            @RequestBody EstablishmentRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.saveMine(userId, dto));
    }

    @GetMapping("/establishment")
    public ResponseEntity<EstablishmentEntity> getEstablishment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.findMine(userId));
    }

    @PostMapping("/services")
    public ResponseEntity<PetServiceEntity> postService(
            @RequestBody PetServiceRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.status(201).body(service.addService(userId, dto));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        service.deactivateService(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/availability")
    public ResponseEntity<List<AvailabilityEntity>> putAvailability(
            @RequestBody List<AvailabilityRequest> dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.replaceAvailability(userId, dto));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityEntity>> getAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.listAvailability(service.findMine(userId).getId()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception ex) {
        return ResponseEntity.status(500).body("Ops!!! Algo deu errado. - " + ex.getMessage());
    }
}
