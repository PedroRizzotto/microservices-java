package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.AppointmentRequest;
import br.edu.atitus.agendaapi.dtos.AppointmentResponse;
import br.edu.atitus.agendaapi.dtos.PayRequest;
import br.edu.atitus.agendaapi.services.AppointmentService;
import br.edu.atitus.agendaapi.servicesimpl.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ws/agenda/appointments")
public class WsAppointmentController {

    private final AppointmentService service;
    private final AuthorizationService authService;

    public WsAppointmentController(AppointmentService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> postAppointment(
            @RequestBody AppointmentRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.status(201).body(AppointmentResponse.from(service.book(userId, userEmail, dto)));
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentResponse>> getMine(
            @PageableDefault(page = 0, size = 10, sort = "startAt", direction = Direction.ASC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.ok(service.findMine(userId, pageable).map(AppointmentResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getOne(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        return ResponseEntity.ok(AppointmentResponse.from(service.getOne(id, userId)));
    }

    @GetMapping("/received")
    public ResponseEntity<Page<AppointmentResponse>> getReceived(
            @PageableDefault(page = 0, size = 10, sort = "startAt", direction = Direction.ASC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.findReceived(userId, pageable).map(AppointmentResponse::from));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        return ResponseEntity.ok(AppointmentResponse.from(service.cancel(id, userId)));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<AppointmentResponse> pay(
            @PathVariable Long id,
            @RequestBody PayRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.ok(AppointmentResponse.from(service.pay(userId, id, dto.method())));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(AppointmentResponse.from(service.complete(userId, id)));
    }
}
