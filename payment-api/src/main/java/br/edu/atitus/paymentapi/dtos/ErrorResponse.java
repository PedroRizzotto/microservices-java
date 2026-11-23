package br.edu.atitus.paymentapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
