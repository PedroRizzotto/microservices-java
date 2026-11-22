package br.edu.atitus.orderapi.controllers;

import br.edu.atitus.orderapi.dtos.OrderStatusRequest;
import br.edu.atitus.orderapi.dtos.PayOrderRequest;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import br.edu.atitus.orderapi.services.OrderService;
import br.edu.atitus.orderapi.servicesimpl.AuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ws/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthorizationService authorization;

    public OrderController(OrderService orderService, AuthorizationService authorization) {
        this.orderService = orderService;
        this.authorization = authorization;
    }

    @GetMapping
    public ResponseEntity<Page<OrderEntity>> listMine(
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @PageableDefault(size = 5, sort = "orderDate", direction = Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(orderService.listMine(userId, targetCurrency.toUpperCase(), pageable));
    }

    @GetMapping("/received")
    public ResponseEntity<Page<OrderEntity>> listReceived(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "orderDate", direction = Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireProvider(userType);
        return ResponseEntity.ok(orderService.listReceived(userId, parseStatus(status, false), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getMine(
            @PathVariable Long id,
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(orderService.getMine(userId, id, targetCurrency.toUpperCase()));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderEntity> pay(
            @PathVariable Long id,
            @RequestBody PayOrderRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(orderService.pay(userId, id, request.method()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderEntity> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(orderService.cancel(userId, id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderEntity> updateStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireProvider(userType);
        return ResponseEntity.ok(orderService.updateStatus(userId, id, parseStatus(request.status(), true)));
    }

    private OrderStatus parseStatus(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido");
            return null;
        }
        try {
            return OrderStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido");
        }
    }
}
