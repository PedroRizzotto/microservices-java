package br.edu.atitus.paymentapi.dtos;

public record PaymentRequest(String referenceType, Long referenceId, Long userId, String method, double amount) {
}
