package br.edu.atitus.productapi.controllers;

import java.util.List;
import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.services.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("products")
public class ProductController {
    private final ProductService service;

    public ProductController(@Qualifier("productServiceJpa") ProductService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(service.listCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable Long id,
            @RequestParam String targetCurrency
    ) throws Exception {
        var response = service.findById(id, targetCurrency);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam String targetCurrency,
            @RequestParam(required = false) Long establishmentId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "name", page = 0) Pageable pageable
    ) throws Exception {
        var response = service.search(establishmentId, search, category, pageable, targetCurrency);
        return ResponseEntity.ok(response);
    }
}
