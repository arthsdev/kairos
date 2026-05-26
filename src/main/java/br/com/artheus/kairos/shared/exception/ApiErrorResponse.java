package br.com.artheus.kairos.shared.exception;

public record ApiErrorResponse(
        int status,
        String message,
        String errorCode,
        long timestamp
) {}
