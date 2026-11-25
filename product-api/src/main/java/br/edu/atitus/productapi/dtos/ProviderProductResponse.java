package br.edu.atitus.productapi.dtos;

import br.edu.atitus.productapi.entities.ProductEntity;

public record ProviderProductResponse(
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
        boolean active,
        Long establishmentId) {

    public static ProviderProductResponse from(ProductEntity entity) {
        return new ProviderProductResponse(
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
                entity.isActive(),
                entity.getEstablishmentId());
    }
}
