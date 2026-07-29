package br.com.artheus.kairos.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthClient authClient;
    private final KeycloakAdminClient keycloakAdminClient;

    public LoginResponse login(LoginRequest request) {
        return authClient.login(request.username(), request.password());
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        return authClient.refresh(request.refreshToken());
    }

    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.email());

        String userId = keycloakAdminClient.createUser(request);

        log.info("User successfully registered with ID: {}", userId);
        return new RegisterResponse(userId, request.username(), request.email());
    }
}