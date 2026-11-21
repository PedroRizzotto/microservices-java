package br.edu.atitus.paymentapi.controllers;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {

    private final PaymentService service;

    public InternalPaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentEntity> pay(@RequestBody PaymentRequest dto) {
        boolean existed = service.exists(dto);
        PaymentEntity payment = service.pay(dto);
        return ResponseEntity.status(existed ? 200 : 201).body(payment);
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
