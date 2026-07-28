package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BaseException {
    public AuthenticationFailedException(String message) {
        super("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, message);
    }

    public AuthenticationFailedException(String message, String errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }
}