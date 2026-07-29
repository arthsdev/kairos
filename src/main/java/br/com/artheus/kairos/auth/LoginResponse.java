package br.com.artheus.kairos.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn
) {}