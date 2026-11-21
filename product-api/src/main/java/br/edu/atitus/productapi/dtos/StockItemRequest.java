package br.edu.atitus.productapi.dtos;

public record StockItemRequest(Long productId, int quantity) {
}
