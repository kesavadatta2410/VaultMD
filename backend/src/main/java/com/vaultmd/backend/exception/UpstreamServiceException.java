package com.vaultmd.backend.exception;

/** Thrown when the ai-service call fails (network error, non-2xx, etc). */
public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
