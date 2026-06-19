package br.com.artheus.kairos.shared.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String errorCode, String defaultMessage) {
        super(errorCode, HttpStatus.BAD_GATEWAY, defaultMessage);
    }

    public ExternalServiceException(String errorCode, String defaultMessage, Throwable cause) {
        super(errorCode, HttpStatus.BAD_GATEWAY, defaultMessage, cause);
    }
}