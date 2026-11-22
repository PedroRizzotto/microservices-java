package br.edu.atitus.orderapi.controllers;

import br.edu.atitus.orderapi.dtos.CartItemRequest;
import br.edu.atitus.orderapi.dtos.CartQuantityRequest;
import br.edu.atitus.orderapi.dtos.CartResponse;
import br.edu.atitus.orderapi.services.CartService;
import br.edu.atitus.orderapi.servicesimpl.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ws/orders/cart")
public class CartController {

    private final CartService cartService;
    private final AuthorizationService authorization;

    public CartController(CartService cartService, AuthorizationService authorization) {
        this.cartService = cartService;
        this.authorization = authorization;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(cartService.getCart(userId, targetCurrency.toUpperCase()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestBody CartItemRequest request,
            @RequestParam(defaultValue = "false") boolean replace,
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(cartService.addItem(userId, request.productId(), request.quantity(),
                replace, targetCurrency.toUpperCase()));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> setQuantity(
            @PathVariable Long productId,
            @RequestBody CartQuantityRequest request,
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(cartService.setQuantity(userId, productId, request.quantity(),
                targetCurrency.toUpperCase()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "BRL") String targetCurrency,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        return ResponseEntity.ok(cartService.removeItem(userId, productId, targetCurrency.toUpperCase()));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Type") Integer userType) {
        authorization.requireCustomer(userType);
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
