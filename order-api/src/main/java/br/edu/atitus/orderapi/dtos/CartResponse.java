package br.edu.atitus.orderapi.dtos;

import java.util.List;

public record CartResponse(
        Long establishmentId,
        List<CartItemResponse> items,
        double total,
        String targetCurrency
) {}
