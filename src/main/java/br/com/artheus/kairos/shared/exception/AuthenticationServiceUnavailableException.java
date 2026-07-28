package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationServiceUnavailableException extends BaseException {
    public AuthenticationServiceUnavailableException(String message) {
        super("AUTH_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}