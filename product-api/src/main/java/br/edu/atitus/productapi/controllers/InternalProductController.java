package br.edu.atitus.productapi.controllers;

import br.edu.atitus.productapi.dtos.ReservedItemResponse;
import br.edu.atitus.productapi.dtos.StockItemRequest;
import br.edu.atitus.productapi.services.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/internal/products/stock")
public class InternalProductController {
    private final StockService service;

    public InternalProductController(StockService service) {
        this.service = service;
    }

    @PostMapping("/reserve")
    public ResponseEntity<List<ReservedItemResponse>> reserve(@RequestBody List<StockItemRequest> items) {
        return ResponseEntity.ok(service.reserve(items));
    }

    @PostMapping("/release")
    public ResponseEntity<Void> release(@RequestBody List<StockItemRequest> items) {
        service.release(items);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(e.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception ex) {
        return ResponseEntity.status(500).body("Ops!!! Algo deu errado. - " + ex.getMessage());
    }
}
