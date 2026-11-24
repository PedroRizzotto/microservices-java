package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.PetRequest;
import br.edu.atitus.agendaapi.entities.PetEntity;
import br.edu.atitus.agendaapi.services.CustomerPetService;
import br.edu.atitus.agendaapi.servicesimpl.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ws/agenda/pets")
public class WsPetController {

    private final CustomerPetService service;
    private final AuthorizationService authService;

    public WsPetController(CustomerPetService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<PetEntity>> getMine(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.ok(service.listMine(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetEntity> getOne(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.ok(service.getMine(userId, id));
    }

    @PostMapping
    public ResponseEntity<PetEntity> post(
            @RequestBody PetRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.status(201).body(service.create(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetEntity> put(
            @PathVariable Long id,
            @RequestBody PetRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        return ResponseEntity.ok(service.update(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authService.requireCustomer(userType);
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
