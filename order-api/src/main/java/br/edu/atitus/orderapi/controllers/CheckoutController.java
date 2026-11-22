package br.edu.atitus.orderapi.controllers;

import br.edu.atitus.orderapi.dtos.CheckoutRequest;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.services.CheckoutService;
import br.edu.atitus.orderapi.servicesimpl.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ws/orders/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final AuthorizationService authorization;

    public CheckoutController(CheckoutService checkoutService, AuthorizationService authorization) {
        this.checkoutService = checkoutService;
        this.authorization = authorization;
    }

    @PostMapping
    public ResponseEntity<OrderEntity> checkout(
            @RequestBody CheckoutRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        if (request.addressId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço obrigatório");
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(userId, request.addressId()));
    }
}
