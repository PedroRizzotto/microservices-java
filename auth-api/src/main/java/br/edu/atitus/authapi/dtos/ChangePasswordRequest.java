package br.edu.atitus.authapi.dtos;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
