package br.com.artheus.kairos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record KeycloakAdminTokenResponse(
        @JsonProperty("access_token") String accessToken
) {}