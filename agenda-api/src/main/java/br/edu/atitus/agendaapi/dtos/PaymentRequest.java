package br.edu.atitus.agendaapi.dtos;

public record PaymentRequest(
        String referenceType,
        Long referenceId,
        Long userId,
        String method,
        double amount
) {}
