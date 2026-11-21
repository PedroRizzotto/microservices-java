package br.edu.atitus.productapi.dtos;

public record ProductRequest(
        String name,
        String brand,
        String model,
        String description,
        String category,
        String currency,
        double price,
        int stock,
        String image
) {
}
