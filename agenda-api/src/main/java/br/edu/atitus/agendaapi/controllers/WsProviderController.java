package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.AvailabilityRequest;
import br.edu.atitus.agendaapi.dtos.AvailabilityResponse;
import br.edu.atitus.agendaapi.dtos.EstablishmentRequest;
import br.edu.atitus.agendaapi.dtos.EstablishmentResponse;
import br.edu.atitus.agendaapi.dtos.PetServiceRequest;
import br.edu.atitus.agendaapi.dtos.PetServiceResponse;
import br.edu.atitus.agendaapi.services.EstablishmentService;
import br.edu.atitus.agendaapi.servicesimpl.AuthorizationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<EstablishmentResponse> putEstablishment(
            @RequestBody EstablishmentRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(EstablishmentResponse.from(service.saveMine(userId, dto)));
    }

    @GetMapping("/establishment")
    public ResponseEntity<EstablishmentResponse> getEstablishment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(EstablishmentResponse.from(service.findMine(userId)));
    }

    @PostMapping("/services")
    public ResponseEntity<PetServiceResponse> postService(
            @RequestBody PetServiceRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.status(201).body(PetServiceResponse.from(service.addService(userId, dto)));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<PetServiceResponse> putService(
            @PathVariable Long id,
            @RequestBody PetServiceRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(PetServiceResponse.from(service.updateService(userId, id, dto)));
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
    public ResponseEntity<List<AvailabilityResponse>> putAvailability(
            @RequestBody List<AvailabilityRequest> dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.replaceAvailability(userId, dto).stream().map(AvailabilityResponse::from).toList());
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.listAvailability(service.findMine(userId).getId()).stream().map(AvailabilityResponse::from).toList());
    }
}
