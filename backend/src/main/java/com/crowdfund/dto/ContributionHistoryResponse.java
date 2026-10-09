package com.crowdfund.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing an enriched contribution ledger item for contributor history.
 */
public record ContributionHistoryResponse(
        String id,
        String campaignId,
        String campaignTitle,
        BigDecimal amount,
        boolean simulated,
        Instant createdAt,
        String campaignStatus,
        BigDecimal campaignTarget,
        BigDecimal campaignCollected,
        int campaignPercentage
) {}
