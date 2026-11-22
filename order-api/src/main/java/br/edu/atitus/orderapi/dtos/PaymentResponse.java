package br.edu.atitus.orderapi.dtos;

public record PaymentResponse(Long id, String status, String transactionCode) {}
