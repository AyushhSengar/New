package com.crowdfund.dto;

import java.math.BigDecimal;

/**
 * DTO containing aggregate metrics for the CREATOR dashboard.
 */
public record CreatorStatsResponse(
        long totalCampaigns,
        long pendingCampaigns,
        long liveCampaigns,
        long completedCampaigns,
        long rejectedCampaigns,
        BigDecimal totalRaised,
        BigDecimal totalTarget
) {}
