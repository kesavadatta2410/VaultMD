package com.vaultmd.backend.dto;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {

    private final int status;
    private final String error;
    private final Instant timestamp = Instant.now();
    private Map<String, String> fieldErrors;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
    }

    public ErrorResponse(int status, String error, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
