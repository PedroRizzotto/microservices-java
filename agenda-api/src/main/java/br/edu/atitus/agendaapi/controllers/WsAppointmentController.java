package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.AppointmentRequest;
import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.services.AppointmentService;
import br.edu.atitus.agendaapi.servicesimpl.AuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<AppointmentEntity> postAppointment(
            @RequestBody AppointmentRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        return ResponseEntity.status(201).body(service.book(userId, userEmail, dto));
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentEntity>> getMine(
            @PageableDefault(page = 0, size = 10, sort = "startAt", direction = Direction.ASC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        return ResponseEntity.ok(service.findMine(userId, pageable));
    }

    @GetMapping("/received")
    public ResponseEntity<Page<AppointmentEntity>> getReceived(
            @PageableDefault(page = 0, size = 10, sort = "startAt", direction = Direction.ASC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireProvider(userType);
        return ResponseEntity.ok(service.findReceived(userId, pageable));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentEntity> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) {
        return ResponseEntity.ok(service.cancel(id, userId));
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
