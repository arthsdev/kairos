package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {
    private static final String DEFAULT_ERROR_CODE = "FORBIDDEN_EXCEPTION";
    public ForbiddenException(String message) {
        super(DEFAULT_ERROR_CODE, HttpStatus.FORBIDDEN, message);
    }
}
