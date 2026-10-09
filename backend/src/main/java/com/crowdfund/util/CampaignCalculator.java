package com.crowdfund.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain calculation engine for campaign funding progress and thresholds.
 */
public final class CampaignCalculator {

    private CampaignCalculator() {}

    /**
     * Calculates completion percentage rounded to integer (0..100).
     */
    public static int calculatePercentage(BigDecimal collected, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (collected == null || collected.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal ratio = collected.divide(target, 4, RoundingMode.HALF_UP);
        int pct = ratio.multiply(BigDecimal.valueOf(100)).intValue();
        return Math.min(100, Math.max(0, pct));
    }

    /**
     * Calculates remaining amount required to fully fund a campaign.
     */
    public static BigDecimal calculateRemaining(BigDecimal target, BigDecimal collected) {
        if (target == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal col = collected != null ? collected : BigDecimal.ZERO;
        BigDecimal diff = target.subtract(col);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    /**
     * Determines whether target has been reached or surpassed.
     */
    public static boolean isTargetReached(BigDecimal target, BigDecimal collected) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return collected != null && collected.compareTo(target) >= 0;
    }
}
