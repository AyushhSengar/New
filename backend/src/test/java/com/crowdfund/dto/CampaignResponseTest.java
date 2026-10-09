package com.crowdfund.dto;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.crowdfund.model.Campaign;

class CampaignResponseTest {

    @Test
    void testFromModel() {
        Campaign campaign = new Campaign(
                "c123",
                "Solar Lamp Initiative",
                "Providing clean lamps to rural schools",
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                "creator_456",
                "Jane Doe",
                "APPROVED",
                "",
                Instant.now(),
                Instant.now()
        );

        CampaignResponse response = CampaignResponse.fromModel(campaign);

        assertNotNull(response);
        assertEquals("c123", response.id());
        assertEquals("Solar Lamp Initiative", response.title());
        assertEquals(50, response.percentage());
        assertEquals(new BigDecimal("2500.00"), response.remainingAmount());
        assertFalse(response.isFullyFunded());
        assertTrue(response.isAcceptingContributions());
    }

    @Test
    void testFromNullModel() {
        assertNull(CampaignResponse.fromModel(null));
    }
}
