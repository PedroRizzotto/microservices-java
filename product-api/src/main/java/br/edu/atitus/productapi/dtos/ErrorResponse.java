package br.edu.atitus.productapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
