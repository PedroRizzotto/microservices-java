package br.edu.atitus.orderapi.dtos;

public record ReservedItemResponse(
        Long productId,
        String name,
        int quantity,
        double price,
        String currency,
        Long establishmentId,
        Long providerId
) {}
