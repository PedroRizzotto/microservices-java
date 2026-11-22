package br.edu.atitus.productapi.controllers;

import br.edu.atitus.productapi.dtos.ReservedItemResponse;
import br.edu.atitus.productapi.dtos.StockItemRequest;
import br.edu.atitus.productapi.services.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
