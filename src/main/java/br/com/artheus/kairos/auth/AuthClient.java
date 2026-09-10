package br.com.artheus.kairos.auth;

import br.com.artheus.kairos.shared.exception.AuthenticationFailedException;
import br.com.artheus.kairos.shared.exception.AuthenticationServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthClient {

    private final WebClient webClient;
    private final String keycloakUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public AuthClient(
            WebClient webClient,
            @Value("${keycloak.url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret) {
        this.webClient = webClient;
        this.keycloakUrl = keycloakUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public LoginResponse login(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);

        return requestToken(formData, "Invalid username or password.");
    }

    public LoginResponse refresh(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        return requestToken(formData, "Invalid or expired refresh token.");
    }

    private LoginResponse requestToken(MultiValueMap<String, String> formData, String invalidCredentialsMessage) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);

        KeycloakTokenResponse keycloakResponse = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new AuthenticationFailedException(invalidCredentialsMessage))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new AuthenticationServiceUnavailableException("Authentication service is temporarily unavailable."))
                )
                .bodyToMono(KeycloakTokenResponse.class)
                .block();

        if (keycloakResponse == null || keycloakResponse.accessToken() == null) {
            throw new AuthenticationServiceUnavailableException("Identity provider returned an empty token response.");
        }

        return new LoginResponse(
                keycloakResponse.accessToken(),
                keycloakResponse.refreshToken(),
                keycloakResponse.expiresIn(),
                keycloakResponse.refreshExpiresIn()
        );
    }
}