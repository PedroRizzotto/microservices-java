package br.edu.atitus.agendaapi.controllers;

import br.edu.atitus.agendaapi.dtos.EstablishmentResponse;
import br.edu.atitus.agendaapi.entities.EstablishmentEntity;
import br.edu.atitus.agendaapi.repositories.EstablishmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/establishments")
public class InternalEstablishmentController {

    private final EstablishmentRepository establishmentRepository;

    public InternalEstablishmentController(EstablishmentRepository establishmentRepository) {
        this.establishmentRepository = establishmentRepository;
    }

    @GetMapping("/by-provider/{providerId}")
    public ResponseEntity<EstablishmentResponse> getByProvider(@PathVariable Long providerId) {
        EstablishmentEntity establishment = establishmentRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));
        return ResponseEntity.ok(EstablishmentResponse.from(establishment));
    }
}
