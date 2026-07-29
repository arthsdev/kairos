package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {

    private static final String DEFAULT_ERROR_CODE = "USER_ALREADY_EXISTS";

    public UserAlreadyExistsException(String message) {
        super(DEFAULT_ERROR_CODE, HttpStatus.CONFLICT, message);
    }
}