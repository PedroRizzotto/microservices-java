package br.edu.atitus.paymentapi.dtos;

import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.entities.PaymentMethod;
import br.edu.atitus.paymentapi.entities.PaymentReferenceType;
import br.edu.atitus.paymentapi.entities.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        PaymentReferenceType referenceType,
        Long referenceId,
        PaymentMethod method,
        double amount,
        String currency,
        PaymentStatus status,
        String transactionCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {

    public static PaymentResponse from(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getMethod(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getTransactionCode(),
                entity.getCreatedAt());
    }
}
