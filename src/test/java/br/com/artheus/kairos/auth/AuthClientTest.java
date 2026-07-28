package br.com.artheus.kairos.auth;

import br.com.artheus.kairos.shared.exception.AuthenticationFailedException;
import br.com.artheus.kairos.shared.exception.AuthenticationServiceUnavailableException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthClientTest {

    private MockWebServer mockWebServer;
    private AuthClient authClient;

    private static final String REALM = "kairos-realm";
    private static final String CLIENT_ID = "kairos-client";
    private static final String CLIENT_SECRET = "secret123";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder().build();

        authClient = new AuthClient(
                webClient,
                baseUrl,
                REALM,
                CLIENT_ID,
                CLIENT_SECRET
        );
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    @Test
    @DisplayName("Should return LoginResponse on successful authentication (200 OK)")
    void login_Success() throws InterruptedException {
        // Arrange
        String mockJsonResponse = """
                {
                    "access_token": "mocked-access-token",
                    "expires_in": 300,
                    "refresh_expires_in": 1800,
                    "refresh_token": "mocked-refresh-token",
                    "token_type": "Bearer"
                }
                """;

        MockResponse mockResponse = new MockResponse.Builder()
                .code(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mockJsonResponse)
                .build();

        mockWebServer.enqueue(mockResponse);

        // Act
        LoginResponse response = authClient.login("userTest", "password123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mocked-access-token");
        assertThat(response.refreshToken()).isEqualTo("mocked-refresh-token");

        // Inspect HTTP Request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getUrl()).isNotNull();
        assertThat(recordedRequest.getUrl().encodedPath())
                .isEqualTo("/realms/" + REALM + "/protocol/openid-connect/token");

        assertThat(recordedRequest.getBody()).isNotNull();
        String requestBody = recordedRequest.getBody().utf8();

        assertThat(requestBody)
                .contains("grant_type=password")
                .contains("client_id=" + CLIENT_ID)
                .contains("username=userTest");
    }

    @Test
    @DisplayName("Should throw AuthenticationFailedException on 4xx Client Error")
    void login_ThrowsAuthenticationFailedException_On4xx() {
        // Arrange
        MockResponse mockResponse = new MockResponse.Builder()
                .code(401)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"error\": \"invalid_grant\"}")
                .build();

        mockWebServer.enqueue(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> authClient.login("userTest", "wrongPassword"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username or password.");
    }

    @Test
    @DisplayName("Should throw AuthenticationServiceUnavailableException on 5xx Server Error")
    void login_ThrowsAuthenticationServiceUnavailableException_On5xx() {
        // Arrange
        MockResponse mockResponse = new MockResponse.Builder()
                .code(503)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"error\": \"service_unavailable\"}")
                .build();

        mockWebServer.enqueue(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> authClient.login("userTest", "password123"))
                .isInstanceOf(AuthenticationServiceUnavailableException.class)
                .hasMessage("Authentication service is temporarily unavailable.");
    }
}