package com.crowdfund.util;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.crowdfund.exception.AppException;

class DomainValidatorTest {

    @Test
    void testValidateRole() {
        assertDoesNotThrow(() -> DomainValidator.validateRole("ADMIN"));
        assertDoesNotThrow(() -> DomainValidator.validateRole("CREATOR"));
        assertDoesNotThrow(() -> DomainValidator.validateRole("CONTRIBUTOR"));
        assertDoesNotThrow(() -> DomainValidator.validateRole("admin"));

        assertThrows(AppException.class, () -> DomainValidator.validateRole("SUPERUSER"));
        assertThrows(AppException.class, () -> DomainValidator.validateRole(null));
    }

    @Test
    void testValidateRegistrationRole() {
        assertDoesNotThrow(() -> DomainValidator.validateRegistrationRole("CREATOR"));
        assertDoesNotThrow(() -> DomainValidator.validateRegistrationRole("CONTRIBUTOR"));

        assertThrows(AppException.class, () -> DomainValidator.validateRegistrationRole("ADMIN"));
        assertThrows(AppException.class, () -> DomainValidator.validateRegistrationRole("MODERATOR"));
    }

    @Test
    void testValidateContributionAmount() {
        assertDoesNotThrow(() -> DomainValidator.validateContributionAmount(new BigDecimal("50.00"), new BigDecimal("100.00")));
        assertDoesNotThrow(() -> DomainValidator.validateContributionAmount(new BigDecimal("100"), new BigDecimal("100.00")));

        assertThrows(AppException.class, () -> DomainValidator.validateContributionAmount(BigDecimal.ZERO, new BigDecimal("100")));
        assertThrows(AppException.class, () -> DomainValidator.validateContributionAmount(new BigDecimal("-10"), new BigDecimal("100")));
        assertThrows(AppException.class, () -> DomainValidator.validateContributionAmount(new BigDecimal("10.123"), new BigDecimal("100"))); // >2 scale
        assertThrows(AppException.class, () -> DomainValidator.validateContributionAmount(new BigDecimal("150"), new BigDecimal("100"))); // exceeds remaining
    }

    @Test
    void testValidateCampaignStatusTransition() {
        assertDoesNotThrow(() -> DomainValidator.validateCampaignStatusTransition("PENDING", "APPROVED"));
        assertDoesNotThrow(() -> DomainValidator.validateCampaignStatusTransition("PENDING", "REJECTED"));

        assertThrows(AppException.class, () -> DomainValidator.validateCampaignStatusTransition("APPROVED", "REJECTED"));
        assertThrows(AppException.class, () -> DomainValidator.validateCampaignStatusTransition("REJECTED", "APPROVED"));
        assertThrows(AppException.class, () -> DomainValidator.validateCampaignStatusTransition("COMPLETED", "APPROVED"));
    }
}
