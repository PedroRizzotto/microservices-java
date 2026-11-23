package br.edu.atitus.authapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
