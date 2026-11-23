package br.edu.atitus.agendaapi.dtos;

public record ErrorResponse(int status, String error, String message, String timestamp) {
}
