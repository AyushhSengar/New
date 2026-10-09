package com.crowdfund.model;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CampaignModelTest {

    @Test
    void testDomainCalculations() {
        Campaign c = new Campaign();
        c.setId("c1");
        c.setTitle("Eco Project");
        c.setDescription("Save trees");
        c.setTargetAmount(new BigDecimal("1000.00"));
        c.setCollectedAmount(new BigDecimal("400.00"));
        c.setStatus("APPROVED");

        assertEquals(40, c.calculatePercentage());
        assertEquals(new BigDecimal("600.00"), c.calculateRemainingAmount());
        assertFalse(c.isFullyFunded());
        assertTrue(c.isAcceptingContributions());

        // Update collected amount to reach target
        c.setCollectedAmount(new BigDecimal("1000.00"));
        assertEquals(100, c.calculatePercentage());
        assertEquals(BigDecimal.ZERO, c.calculateRemainingAmount());
        assertTrue(c.isFullyFunded());
        assertFalse(c.isAcceptingContributions());

        // Completed status
        c.setStatus("COMPLETED");
        assertTrue(c.isFullyFunded());
        assertFalse(c.isAcceptingContributions());

        // Edge case: zero target
        c.setTargetAmount(BigDecimal.ZERO);
        assertEquals(0, c.calculatePercentage());
    }

    @Test
    void testEqualityAndGetters() {
        Instant now = Instant.now();
        Campaign c1 = new Campaign("c1", "Title", "Desc", BigDecimal.TEN, BigDecimal.ONE, "cr1", "Creator", "PENDING", "", now, now);
        Campaign c2 = new Campaign("c1", "Title2", "Desc2", BigDecimal.TEN, BigDecimal.ONE, "cr1", "Creator", "PENDING", "", now, now);
        Campaign c3 = new Campaign("c2", "Title", "Desc", BigDecimal.TEN, BigDecimal.ONE, "cr1", "Creator", "PENDING", "", now, now);

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertEquals("cr1", c1.getCreatorId());
        assertEquals("Creator", c1.getCreatorName());
        assertEquals(now, c1.getCreatedAt());
        assertEquals(now, c1.getUpdatedAt());
    }
}
