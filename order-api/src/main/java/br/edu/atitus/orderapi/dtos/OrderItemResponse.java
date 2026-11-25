package br.edu.atitus.orderapi.dtos;

import br.edu.atitus.orderapi.entities.OrderItemEntity;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        double priceAtPurchase,
        String currencyAtPurchase,
        double convertedPrice) {

    public static OrderItemResponse from(OrderItemEntity entity) {
        return new OrderItemResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getPriceAtPurchase(),
                entity.getCurrencyAtPurchase(),
                entity.getConvertedPrice());
    }
}
