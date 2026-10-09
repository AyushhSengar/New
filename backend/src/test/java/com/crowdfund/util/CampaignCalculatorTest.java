package com.crowdfund.util;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CampaignCalculatorTest {

    @Test
    void testCalculatePercentage() {
        assertEquals(50, CampaignCalculator.calculatePercentage(new BigDecimal("500"), new BigDecimal("1000")));
        assertEquals(0, CampaignCalculator.calculatePercentage(BigDecimal.ZERO, new BigDecimal("1000")));
        assertEquals(100, CampaignCalculator.calculatePercentage(new BigDecimal("1000"), new BigDecimal("1000")));
        assertEquals(100, CampaignCalculator.calculatePercentage(new BigDecimal("1500"), new BigDecimal("1000"))); // clamped to 100
        assertEquals(33, CampaignCalculator.calculatePercentage(new BigDecimal("100"), new BigDecimal("300")));
        assertEquals(0, CampaignCalculator.calculatePercentage(new BigDecimal("100"), BigDecimal.ZERO));
    }

    @Test
    void testCalculateRemaining() {
        assertEquals(new BigDecimal("500"), CampaignCalculator.calculateRemaining(new BigDecimal("1000"), new BigDecimal("500")));
        assertEquals(BigDecimal.ZERO, CampaignCalculator.calculateRemaining(new BigDecimal("1000"), new BigDecimal("1000")));
        assertEquals(BigDecimal.ZERO, CampaignCalculator.calculateRemaining(new BigDecimal("1000"), new BigDecimal("1200")));
    }

    @Test
    void testIsTargetReached() {
        assertFalse(CampaignCalculator.isTargetReached(new BigDecimal("1000"), new BigDecimal("999.99")));
        assertTrue(CampaignCalculator.isTargetReached(new BigDecimal("1000"), new BigDecimal("1000.00")));
        assertTrue(CampaignCalculator.isTargetReached(new BigDecimal("1000"), new BigDecimal("1500.00")));
        assertFalse(CampaignCalculator.isTargetReached(BigDecimal.ZERO, new BigDecimal("100")));
    }
}
