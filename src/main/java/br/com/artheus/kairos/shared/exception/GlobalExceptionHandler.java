package br.com.artheus.kairos.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // CAPTURE THE ONES THAT EXTENDS BASEEXCEPTION (ResourceNotFound, Business, etc)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiErrorResponse> handleBaseException(BaseException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getHttpStatus().value(),
                ex.getMessage(),
                ex.getErrorCode(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, ex.getHttpStatus());
    }

    // GENERIC ERRORS (ERROR 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal error occurred.",
                "INTERNAL_SERVER_ERROR",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
