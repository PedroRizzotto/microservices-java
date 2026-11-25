package br.edu.atitus.orderapi.dtos;

import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime orderDate,
        OrderStatus status,
        Long establishmentId,
        String customerEmail,
        double totalAmount,
        double totalConvertedPrice,
        String deliveryZipCode,
        String deliveryStreet,
        String deliveryNumber,
        String deliveryComplement,
        String deliveryDistrict,
        String deliveryCity,
        String deliveryState,
        Long paymentId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime paidAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getOrderDate(),
                entity.getStatus(),
                entity.getEstablishmentId(),
                entity.getCustomerEmail(),
                entity.getTotalAmount(),
                entity.getTotalConvertedPrice(),
                entity.getDeliveryZipCode(),
                entity.getDeliveryStreet(),
                entity.getDeliveryNumber(),
                entity.getDeliveryComplement(),
                entity.getDeliveryDistrict(),
                entity.getDeliveryCity(),
                entity.getDeliveryState(),
                entity.getPaymentId(),
                entity.getPaidAt(),
                entity.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
