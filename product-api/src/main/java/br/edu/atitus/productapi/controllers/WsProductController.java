package br.edu.atitus.productapi.controllers;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.dtos.ProviderProductResponse;
import br.edu.atitus.productapi.services.ProductService;
import br.edu.atitus.productapi.servicesimpl.AuthorizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ws/products")
public class WsProductController {
    private final ProductService service;
    private final AuthorizationService authService;

    public WsProductController(@Qualifier("productServiceJpa") ProductService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ProviderProductResponse> postProduct(
            @RequestBody ProductRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {

        authService.requireProvider(userType);

        return ResponseEntity.status(201).body(ProviderProductResponse.from(service.create(userId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderProductResponse> putProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {

        authService.requireProvider(userType);

        return ResponseEntity.ok(ProviderProductResponse.from(service.update(userId, id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {

        authService.requireProvider(userType);

        service.deactivate(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ProviderProductResponse>> getMine(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType,
            @PageableDefault(size = 10, sort = "name", page = 0) Pageable pageable) {

        authService.requireProvider(userType);

        return ResponseEntity.ok(service.findMine(userId, pageable).map(ProviderProductResponse::from));
    }
}
