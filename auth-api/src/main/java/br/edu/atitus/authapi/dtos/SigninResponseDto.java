package br.edu.atitus.authapi.dtos;

public record SigninResponseDto(MeResponse user, String token) {
}
