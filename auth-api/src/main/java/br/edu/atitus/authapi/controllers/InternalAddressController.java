package br.edu.atitus.authapi.controllers;

import br.edu.atitus.authapi.entities.AddressEntity;
import br.edu.atitus.authapi.services.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
