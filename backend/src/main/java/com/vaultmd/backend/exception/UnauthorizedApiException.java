package com.vaultmd.backend.exception;

public class UnauthorizedApiException extends RuntimeException {
    public UnauthorizedApiException(String message) {
        super(message);
    }
}
