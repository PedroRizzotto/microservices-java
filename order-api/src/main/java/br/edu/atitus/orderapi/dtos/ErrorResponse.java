package br.edu.atitus.orderapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
