package com.vaultmd.backend.exception;

public class NotFoundApiException extends RuntimeException {
    public NotFoundApiException(String message) {
        super(message);
    }
}
