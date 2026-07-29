package br.com.artheus.kairos.auth;

public record RegisterResponse(
        String userId,
        String username,
        String email
) {}