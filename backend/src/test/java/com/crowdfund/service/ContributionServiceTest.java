package com.crowdfund.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.security.AuthUser;
import com.google.cloud.firestore.Firestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private Firestore db;

    @InjectMocks
    private ContributionService contributionService;

    @Test
    @DisplayName("contribute blocks non-CONTRIBUTOR roles")
    void testContributeRoleRestriction() {
        AuthUser creator = new AuthUser("uid1", "creator@test.com", "Creator", "CREATOR");
        AmountRequest req = new AmountRequest(new BigDecimal("100.00"));

        AppException ex = assertThrows(AppException.class,
                () -> contributionService.contribute(creator, "camp123", req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("contribute rejects invalid campaign IDs")
    void testContributeInvalidId() {
        AuthUser contributor = new AuthUser("uid2", "donor@test.com", "Donor", "CONTRIBUTOR");
        AmountRequest req = new AmountRequest(new BigDecimal("50.00"));

        AppException ex = assertThrows(AppException.class,
                () -> contributionService.contribute(contributor, "invalid/id", req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("contribute rejects zero, negative or null amounts")
    void testContributeInvalidAmount() {
        AuthUser contributor = new AuthUser("uid2", "donor@test.com", "Donor", "CONTRIBUTOR");

        AppException exNull = assertThrows(AppException.class,
                () -> contributionService.contribute(contributor, "camp123", new AmountRequest(null)));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatus());

        AppException exZero = assertThrows(AppException.class,
                () -> contributionService.contribute(contributor, "camp123", new AmountRequest(BigDecimal.ZERO)));
        assertEquals(HttpStatus.BAD_REQUEST, exZero.getStatus());
    }
}
