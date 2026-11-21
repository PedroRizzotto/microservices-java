package br.edu.atitus.productapi.dtos;

public record ReservedItemResponse(Long productId, String name, int quantity, double price, String currency,
                                   Long establishmentId, Long providerId) {
}
