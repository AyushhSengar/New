package com.crowdfund.dto;

import java.time.Instant;

/**
 * DTO representing user account info for admin management.
 */
public record UserSummaryResponse(
        String uid,
        String name,
        String email,
        String role,
        Instant createdAt
) {}
