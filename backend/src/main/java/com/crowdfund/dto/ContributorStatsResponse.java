package com.crowdfund.dto;

import java.math.BigDecimal;

/**
 * DTO containing aggregate metrics for the CONTRIBUTOR dashboard.
 */
public record ContributorStatsResponse(
        long openCampaignsCount,
        long totalContributionsCount,
        long uniqueCampaignsSupported,
        BigDecimal totalContributedAmount
) {}
