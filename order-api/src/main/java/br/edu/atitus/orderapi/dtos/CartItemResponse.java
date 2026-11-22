package br.edu.atitus.orderapi.dtos;

public record CartItemResponse(
        Long productId,
        String name,
        String image,
        int quantity,
        double unitPrice,
        double subtotal,
        boolean available
) {}
