package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    private static final String DEFAULT_ERROR_CODE = "BUSINESS_EXCEPTION";
    public BusinessException(String message) {
        super(DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
