package br.com.artheus.kairos.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthClient authClient;

    public LoginResponse login(LoginRequest request) {
        return authClient.login(request.username(), request.password());
    }
}