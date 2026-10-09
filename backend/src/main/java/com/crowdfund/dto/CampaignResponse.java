package com.crowdfund.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.crowdfund.model.Campaign;

/**
 * Detailed DTO for campaign view with calculated business metrics.
 */
public record CampaignResponse(
        String id,
        String title,
        String description,
        BigDecimal targetAmount,
        BigDecimal collectedAmount,
        BigDecimal remainingAmount,
        int percentage,
        String creatorId,
        String creatorName,
        String status,
        String rejectionReason,
        boolean isFullyFunded,
        boolean isAcceptingContributions,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignResponse fromModel(Campaign campaign) {
        if (campaign == null) {
            return null;
        }
        return new CampaignResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getTargetAmount(),
                campaign.getCollectedAmount(),
                campaign.calculateRemainingAmount(),
                campaign.calculatePercentage(),
                campaign.getCreatorId(),
                campaign.getCreatorName(),
                campaign.getStatus(),
                campaign.getRejectionReason(),
                campaign.isFullyFunded(),
                campaign.isAcceptingContributions(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
