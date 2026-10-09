package com.crowdfund.util;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

/**
 * Encapsulates domain validation rules for FundHub entities and business transactions.
 */
public final class DomainValidator {

    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "CREATOR", "CONTRIBUTOR");
    public static final Set<String> REGISTRABLE_ROLES = Set.of("CREATOR", "CONTRIBUTOR");
    public static final Set<String> CAMPAIGN_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "COMPLETED");

    public static final int MAX_CAMPAIGN_TITLE_LENGTH = 120;
    public static final int MAX_CAMPAIGN_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_REJECTION_REASON_LENGTH = 500;
    public static final int MAX_USER_NAME_LENGTH = 80;

    private DomainValidator() {}

    public static void validateRole(String role) {
        if (role == null || !ALLOWED_ROLES.contains(role.trim().toUpperCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid role. Allowed roles are ADMIN, CREATOR, CONTRIBUTOR.");
        }
    }

    public static void validateRegistrationRole(String role) {
        if (role == null || !REGISTRABLE_ROLES.contains(role.trim().toUpperCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid registration role. Allowed roles are CREATOR and CONTRIBUTOR.");
        }
    }

    public static void validateContributionAmount(BigDecimal amount, BigDecimal remaining) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Contribution amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Contribution amount cannot have more than 2 decimal places.");
        }
        if (remaining != null && amount.compareTo(remaining) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Maximum contribution is " + remaining.toPlainString() + ".");
        }
    }

    public static void validateCampaignStatusTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Campaign status cannot be null.");
        }
        String current = currentStatus.trim().toUpperCase();
        String target = targetStatus.trim().toUpperCase();

        if (!CAMPAIGN_STATUSES.contains(target)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unknown campaign status: " + target);
        }

        if (("APPROVED".equals(target) || "REJECTED".equals(target)) && !"PENDING".equals(current)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Only pending campaigns can be approved or rejected. This campaign is already " + current + ".");
        }
    }
}
