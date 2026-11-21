package br.edu.atitus.paymentapi.controllers;

import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.services.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ws/payments")
public class WsPaymentController {

    private final PaymentService service;

    public WsPaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentEntity>> getMine(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.listMine(userId, pageable));
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
