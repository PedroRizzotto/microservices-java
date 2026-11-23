package br.edu.atitus.currencyapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
