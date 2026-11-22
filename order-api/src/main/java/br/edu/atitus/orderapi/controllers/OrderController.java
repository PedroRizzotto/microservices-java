package br.edu.atitus.orderapi.controllers;

import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ws/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderEntity>> listOrdersByUser(
            @RequestParam String targetCurrency,
            @PageableDefault(page = 0,size = 5,sort = "orderDate", direction = Direction.ASC)
            Pageable pageable,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type")Integer userType) {
        targetCurrency = targetCurrency.toUpperCase();
        Page<OrderEntity> orders = orderService.findOrdersByCustomerId(userId, targetCurrency, pageable);
        return ResponseEntity.ok(orders);
    }
}
