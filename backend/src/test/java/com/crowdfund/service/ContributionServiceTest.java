package com.crowdfund.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.dto.ContributionHistoryResponse;
import com.crowdfund.exception.AppException;
import com.crowdfund.model.Campaign;
import com.crowdfund.model.Donation;
import com.crowdfund.security.AuthUser;
import com.google.cloud.firestore.Firestore;

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

    @Test
    @DisplayName("enrichDonations calculates progress and merges campaign info")
    void testEnrichDonations() {
        Instant now = Instant.now();
        Donation d1 = new Donation("d1", "c1", "Camp 1", "u1", "Donor", "d@e.com", new BigDecimal("100"), true, now.minusSeconds(10));
        Donation d2 = new Donation("d2", "c2", "Camp 2", "u1", "Donor", "d@e.com", new BigDecimal("250"), true, now);

        Campaign c1 = new Campaign("c1", "Camp 1", "Desc", new BigDecimal("1000"), new BigDecimal("500"), "cr1", "Creator", "APPROVED", "", now, now);
        Campaign c2 = new Campaign("c2", "Camp 2", "Desc", new BigDecimal("500"), new BigDecimal("500"), "cr2", "Creator", "COMPLETED", "", now, now);

        List<ContributionHistoryResponse> enriched = contributionService.enrichDonations(List.of(d1, d2), Map.of("c1", c1, "c2", c2));

        assertEquals(2, enriched.size());
        // Sorted by date descending: d2 first, d1 second
        assertEquals("d2", enriched.get(0).id());
        assertEquals("Camp 2", enriched.get(0).campaignTitle());
        assertEquals(100, enriched.get(0).campaignPercentage());
        assertEquals("COMPLETED", enriched.get(0).campaignStatus());

        assertEquals("d1", enriched.get(1).id());
        assertEquals(50, enriched.get(1).campaignPercentage());
        assertEquals("APPROVED", enriched.get(1).campaignStatus());
        assertTrue(enriched.get(1).simulated());
    }
}
