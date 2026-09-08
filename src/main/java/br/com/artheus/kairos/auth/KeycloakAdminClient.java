package br.com.artheus.kairos.auth;

import br.com.artheus.kairos.shared.exception.AuthenticationServiceUnavailableException;
import br.com.artheus.kairos.shared.exception.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Integration client for interacting with the Keycloak Admin REST API.
 * Handles administrative operations such as user provisioning using client credentials.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final WebClient webClient;
    private final String keycloakUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;

    public KeycloakAdminClient(
            WebClient webClient,
            @Value("${keycloak.internal-url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin-client-id}") String adminClientId,
            @Value("${keycloak.admin-client-secret}") String adminClientSecret) {
        this.webClient = webClient;
        this.keycloakUrl = keycloakUrl;
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    /**
     * Creates a new user in Keycloak using the Admin REST API.
     */
    public String createUser(RegisterRequest request) {
        String adminToken = obtainAdminToken();
        KeycloakUserRepresentation payload = KeycloakUserRepresentation.from(request);
        String createUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);

        log.info("Sending user creation request to Keycloak Admin API for email: {}", request.email());

        URI location = webClient.post()
                .uri(createUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                // 1. Business Rule: User or email already registered
                .onStatus(status -> status.isSameCodeAs(HttpStatus.CONFLICT),
                        response -> {
                            log.warn("User creation failed in Keycloak. User already exists for email: {}", request.email());
                            return Mono.error(new UserAlreadyExistsException("User with this email or username already exists"));
                        })
                // 2. Permission/Setup Error: Service Account lacks manage-users role or invalid scope
                .onStatus(status -> status.isSameCodeAs(HttpStatus.FORBIDDEN) || status.isSameCodeAs(HttpStatus.UNAUTHORIZED),
                        response -> {
                            log.error("CRITICAL: Access denied on Keycloak Admin API (Status {}). Verify client permissions and secret!", response.statusCode());
                            return Mono.error(new AuthenticationServiceUnavailableException("Identity provider configuration error"));
                        })
                // 3. Other client-side errors (4xx)
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error during user creation in Keycloak: Status {}", response.statusCode());
                    return Mono.error(new AuthenticationServiceUnavailableException("Failed to process registration in identity provider"));
                })
                // 4. Server-side errors (5xx)
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("Keycloak server error during user creation: Status {}", response.statusCode());
                    return Mono.error(new AuthenticationServiceUnavailableException("Identity provider is currently unavailable"));
                })
                .toBodilessEntity()
                .mapNotNull(response -> response.getHeaders().getLocation())
                // Only map transport/network errors to 503. Java code/deserialization bugs flow through to 500!
                .onErrorMap(WebClientRequestException.class,
                        e -> new AuthenticationServiceUnavailableException("Network error communicating with identity provider"))
                .block();

        if (location == null) {
            log.error("Failed to extract Location header from Keycloak response for email: {}", request.email());
            throw new AuthenticationServiceUnavailableException("Identity provider did not return valid user location");
        }

        String path = location.getPath();
        String createdUserId = path.substring(path.lastIndexOf('/') + 1);

        log.info("User created successfully in Keycloak with ID: {}", createdUserId);
        return createdUserId;
    }

    /**
     * Authenticates the backend service against Keycloak using the client_credentials grant type.
     */
    private String obtainAdminToken() {
        log.debug("Requesting admin access token using client_credentials grant type");
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);

        KeycloakAdminTokenResponse response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", adminClientId)
                        .with("client_secret", adminClientSecret))
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN), res -> {
                    log.error("CRITICAL: Invalid admin client_id or client_secret for Keycloak credentials grant!");
                    return Mono.error(new AuthenticationServiceUnavailableException("Identity provider authentication failed"));
                })
                .onStatus(HttpStatusCode::isError, res -> {
                    log.error("Failed to obtain Keycloak admin token. Status code: {}", res.statusCode());
                    return Mono.error(new AuthenticationServiceUnavailableException("Identity provider authentication failed"));
                })
                .bodyToMono(KeycloakAdminTokenResponse.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new AuthenticationServiceUnavailableException("Service unavailable while retrieving admin token"))
                .block();

        if (response == null || response.accessToken() == null) {
            log.error("Admin token response from Keycloak was null or empty");
            throw new AuthenticationServiceUnavailableException("Identity provider returned empty admin credentials");
        }

        return response.accessToken();
    }
}