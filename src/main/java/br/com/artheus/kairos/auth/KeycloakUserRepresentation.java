package br.com.artheus.kairos.auth;

import java.util.List;

record KeycloakUserRepresentation(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<KeycloakCredential> credentials
) {
    record KeycloakCredential(
            String type,
            String value,
            boolean temporary
    ) {}

    static KeycloakUserRepresentation from(RegisterRequest request) {
        var credential = new KeycloakCredential("password", request.password(), false);
        return new KeycloakUserRepresentation(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                true,
                List.of(credential)
        );
    }
}