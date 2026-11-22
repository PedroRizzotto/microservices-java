package br.edu.atitus.currencyapi.dtos;

// Formato único de erro devolvido por todos os serviços
public record ErrorResponse(int status, String error, String message, String timestamp) {
}
