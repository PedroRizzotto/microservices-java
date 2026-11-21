package br.edu.atitus.productapi.dtos;

import br.edu.atitus.productapi.entities.ProductEntity;

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
) {
    public static ProductResponse fromEntity(ProductEntity entity, String environment, String promotionMessage, String targetCurrency, double convertedPrice) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getBrand(),
                entity.getModel(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getCurrency(),
                entity.getPrice(),
                entity.getStock(),
                entity.getImage(),
                entity.getEstablishmentId(),
                environment,
                promotionMessage,
                targetCurrency,
                convertedPrice
        );
    }
}
