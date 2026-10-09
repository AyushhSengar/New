package com.crowdfund.model;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DonationModelTest {

    @Test
    void testGettersAndEquality() {
        Instant now = Instant.now();
        Donation d1 = new Donation("d1", "c1", "Camp 1", "u1", "Donor", "d@test.com", new BigDecimal("50.00"), true, now);
        Donation d2 = new Donation("d1", "c1", "Camp 1", "u1", "Donor", "d@test.com", new BigDecimal("50.00"), true, now);
        Donation d3 = new Donation("d2", "c1", "Camp 1", "u1", "Donor", "d@test.com", new BigDecimal("50.00"), true, now);

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertEquals("d1", d1.getId());
        assertEquals("c1", d1.getCampaignId());
        assertEquals("Camp 1", d1.getCampaignTitle());
        assertEquals("u1", d1.getContributorId());
        assertEquals("Donor", d1.getContributorName());
        assertEquals("d@test.com", d1.getContributorEmail());
        assertEquals(new BigDecimal("50.00"), d1.getAmount());
        assertTrue(d1.isSimulated());
        assertEquals(now, d1.getCreatedAt());
    }
}
