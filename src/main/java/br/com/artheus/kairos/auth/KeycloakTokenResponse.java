package br.com.artheus.kairos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record KeycloakTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn
) {}