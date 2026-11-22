package br.edu.atitus.orderapi.dtos;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        String model,
        String description,
        String category,
        String currency,
        double price,
        int stock,
        String image,
        Long establishmentId,

        String environment,
        String promotionMessage,
        String targetCurrency,
        double convertedPrice
) {}
