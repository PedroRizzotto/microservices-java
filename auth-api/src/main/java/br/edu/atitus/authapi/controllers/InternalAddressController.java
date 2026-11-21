package br.edu.atitus.authapi.controllers;

import br.edu.atitus.authapi.entities.AddressEntity;
import br.edu.atitus.authapi.services.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/addresses")
public class InternalAddressController {

    private final AddressService service;

    public InternalAddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressEntity> getAddress(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(service.getForUser(id, userId));
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
