package com.crowdfund.dto;

import java.time.Instant;

/**
 * Standardized API error payload.
 */
public record ErrorResponse(
        String error,
        int status,
        Instant timestamp
) {
    public ErrorResponse(String error, int status) {
        this(error, status, Instant.now());
    }
}
