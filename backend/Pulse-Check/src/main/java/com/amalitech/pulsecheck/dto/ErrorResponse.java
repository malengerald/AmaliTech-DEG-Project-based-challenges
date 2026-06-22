package com.amalitech.pulsecheck.dto;

import java.time.Instant;

/**
 * Standard error response shape returned for 404s, validation errors, etc.
 */
public class ErrorResponse {

    private final String error;
    private final Instant timestamp;

    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = Instant.now();
    }

    public String getError() {
        return error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
