package br.edu.atitus.authapi.controllers;

import br.edu.atitus.authapi.dtos.AddressRequest;
import br.edu.atitus.authapi.dtos.MeResponse;
import br.edu.atitus.authapi.entities.AddressEntity;
import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.repositories.UserRepository;
import br.edu.atitus.authapi.services.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/ws/auth")
public class WsAuthController {

    private final UserRepository userRepository;
    private final AddressService addressService;

    public WsAuthController(UserRepository userRepository, AddressService addressService) {
        this.userRepository = userRepository;
        this.addressService = addressService;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@RequestHeader("X-User-Id") Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return ResponseEntity.ok(new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getType().getCode()));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressEntity>> getAddresses(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(addressService.listMine(userId));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressEntity> postAddress(
            @RequestBody AddressRequest dto,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(201).body(addressService.create(userId, dto));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressEntity> putAddress(
            @PathVariable Long id,
            @RequestBody AddressRequest dto,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(addressService.update(userId, id, dto));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        addressService.delete(userId, id);
        return ResponseEntity.noContent().build();
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
