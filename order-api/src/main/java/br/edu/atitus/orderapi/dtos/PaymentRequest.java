package br.edu.atitus.orderapi.dtos;

public record PaymentRequest(
        String referenceType,
        Long referenceId,
        Long userId,
        String method,
        double amount
) {}
