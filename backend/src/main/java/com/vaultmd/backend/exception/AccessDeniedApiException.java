package com.vaultmd.backend.exception;

public class AccessDeniedApiException extends RuntimeException {
    public AccessDeniedApiException(String message) {
        super(message);
    }
}
