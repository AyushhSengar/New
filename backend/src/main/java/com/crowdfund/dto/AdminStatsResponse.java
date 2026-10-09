package com.crowdfund.dto;

import java.math.BigDecimal;

/**
 * DTO containing aggregate platform analytics for the ADMIN dashboard.
 */
public record AdminStatsResponse(
        long pendingCampaigns,
        long approvedCampaigns,
        long rejectedCampaigns,
        long completedCampaigns,
        long totalCampaigns,
        BigDecimal totalRaised,
        long totalUsers,
        long adminCount,
        long creatorCount,
        long contributorCount
) {}
