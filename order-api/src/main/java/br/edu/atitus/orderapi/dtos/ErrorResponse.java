package br.edu.atitus.orderapi.dtos;

// Formato único de erro devolvido por todos os serviços
public record ErrorResponse(int status, String error, String message, String timestamp) {
}
