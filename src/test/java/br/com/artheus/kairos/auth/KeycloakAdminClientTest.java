package br.com.artheus.kairos.auth;

import br.com.artheus.kairos.shared.exception.AuthenticationServiceUnavailableException;
import br.com.artheus.kairos.shared.exception.UserAlreadyExistsException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakAdminClientTest {

    private MockWebServer mockWebServer;
    private KeycloakAdminClient keycloakAdminClient;

    private static final String REALM = "kairos-test";
    private static final String ADMIN_CLIENT_ID = "kairos-admin-service";
    private static final String ADMIN_CLIENT_SECRET = "secret123";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder().build();

        keycloakAdminClient = new KeycloakAdminClient(
                webClient,
                baseUrl,
                REALM,
                ADMIN_CLIENT_ID,
                ADMIN_CLIENT_SECRET
        );
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    private RegisterRequest createSampleRegisterRequest() {
        return new RegisterRequest(
                "johndoe",
                "john.doe@example.com",
                "SecurePassword123!",
                "John",
                "Doe"
        );
    }

    private MockResponse createValidTokenResponse() {
        String tokenJsonResponse = """
                {
                    "access_token": "mocked-admin-access-token",
                    "expires_in": 300,
                    "token_type": "Bearer"
                }
                """;

        return new MockResponse.Builder()
                .code(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(tokenJsonResponse)
                .build();
    }

    @Nested
    @DisplayName("createUser tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should return created user ID on successful registration (201 Created)")
        void createUser_Success() throws InterruptedException {
            // Arrange
            mockWebServer.enqueue(createValidTokenResponse());

            String createdUserId = "a1b2c3d4-5678-90ab-cdef-1234567890ab";
            String locationHeader = mockWebServer.url("/admin/realms/" + REALM + "/users/" + createdUserId).toString();

            MockResponse createUserResponse = new MockResponse.Builder()
                    .code(201)
                    .addHeader(HttpHeaders.LOCATION, locationHeader)
                    .build();

            mockWebServer.enqueue(createUserResponse);

            RegisterRequest request = createSampleRegisterRequest();

            // Act
            String resultUserId = keycloakAdminClient.createUser(request);

            // Assert
            assertThat(resultUserId).isEqualTo(createdUserId);

            // Inspect 1st HTTP Request: Admin Token Retrieval
            RecordedRequest tokenRequest = mockWebServer.takeRequest();
            assertThat(tokenRequest.getMethod()).isEqualTo("POST");
            assertThat(tokenRequest.getUrl()).isNotNull();
            assertThat(tokenRequest.getUrl().encodedPath())
                    .isEqualTo("/realms/" + REALM + "/protocol/openid-connect/token");

            assertThat(tokenRequest.getBody()).isNotNull();
            String tokenRequestBody = tokenRequest.getBody().utf8();
            assertThat(tokenRequestBody)
                    .contains("grant_type=client_credentials")
                    .contains("client_id=" + ADMIN_CLIENT_ID)
                    .contains("client_secret=" + ADMIN_CLIENT_SECRET);

            // Inspect 2nd HTTP Request: User Creation
            RecordedRequest createUserRequest = mockWebServer.takeRequest();
            assertThat(createUserRequest.getMethod()).isEqualTo("POST");
            assertThat(createUserRequest.getUrl()).isNotNull();
            assertThat(createUserRequest.getUrl().encodedPath())
                    .isEqualTo("/admin/realms/" + REALM + "/users");

            assertThat(createUserRequest.getHeaders().get(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer mocked-admin-access-token");

            assertThat(createUserRequest.getBody()).isNotNull();
            String createUserRequestBody = createUserRequest.getBody().utf8();
            assertThat(createUserRequestBody)
                    .contains("johndoe")
                    .contains("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when Keycloak returns 409 Conflict")
        void createUser_ThrowsUserAlreadyExistsException_On409() {
            // Arrange
            mockWebServer.enqueue(createValidTokenResponse());

            MockResponse conflictResponse = new MockResponse.Builder()
                    .code(409)
                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"errorMessage\": \"User exists with same username\"}")
                    .build();

            mockWebServer.enqueue(conflictResponse);

            RegisterRequest request = createSampleRegisterRequest();

            // Act & Assert
            assertThatThrownBy(() -> keycloakAdminClient.createUser(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User with this email or username already exists");

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw AuthenticationServiceUnavailableException when user creation returns 403 Forbidden")
        void createUser_ThrowsAuthenticationServiceUnavailableException_On403() {
            // Arrange
            mockWebServer.enqueue(createValidTokenResponse());

            MockResponse forbiddenResponse = new MockResponse.Builder()
                    .code(403)
                    .build();

            mockWebServer.enqueue(forbiddenResponse);

            RegisterRequest request = createSampleRegisterRequest();

            // Act & Assert
            assertThatThrownBy(() -> keycloakAdminClient.createUser(request))
                    .isInstanceOf(AuthenticationServiceUnavailableException.class)
                    .hasMessage("Identity provider configuration error");

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw AuthenticationServiceUnavailableException and abort when admin token acquisition fails")
        void createUser_ThrowsAuthenticationServiceUnavailableException_OnAdminTokenFailure() {
            // Arrange
            MockResponse failedTokenResponse = new MockResponse.Builder()
                    .code(401)
                    .build();

            mockWebServer.enqueue(failedTokenResponse);

            RegisterRequest request = createSampleRegisterRequest();

            // Act & Assert
            assertThatThrownBy(() -> keycloakAdminClient.createUser(request))
                    .isInstanceOf(AuthenticationServiceUnavailableException.class)
                    .hasMessage("Identity provider authentication failed");

            // Ensures flow aborted immediately without sending the second request
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }
    }
}