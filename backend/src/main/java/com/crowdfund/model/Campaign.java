package com.crowdfund.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a Crowdfunding campaign in FundHub.
 * Contains core campaign attributes and domain calculation logic.
 */
public class Campaign {

    private String id;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal collectedAmount;
    private String creatorId;
    private String creatorName;
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;

    public Campaign() {
        this.targetAmount = BigDecimal.ZERO;
        this.collectedAmount = BigDecimal.ZERO;
        this.status = "PENDING";
        this.rejectionReason = "";
    }

    public Campaign(String id, String title, String description, BigDecimal targetAmount,
                    BigDecimal collectedAmount, String creatorId, String creatorName,
                    String status, String rejectionReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetAmount = targetAmount != null ? targetAmount : BigDecimal.ZERO;
        this.collectedAmount = collectedAmount != null ? collectedAmount : BigDecimal.ZERO;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.status = status != null ? status : "PENDING";
        this.rejectionReason = rejectionReason != null ? rejectionReason : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Domain Business Logic & Calculations ---

    /**
     * Calculates the funding percentage (0 to 100).
     */
    public int calculatePercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (collectedAmount == null || collectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal fraction = collectedAmount.divide(targetAmount, 4, RoundingMode.HALF_UP);
        int percent = fraction.multiply(BigDecimal.valueOf(100)).intValue();
        return Math.min(100, Math.max(0, percent));
    }

    /**
     * Calculates remaining amount required to reach the target.
     */
    public BigDecimal calculateRemainingAmount() {
        if (targetAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal collected = collectedAmount != null ? collectedAmount : BigDecimal.ZERO;
        BigDecimal remaining = targetAmount.subtract(collected);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    /**
     * Determines whether the campaign is fully funded or marked as COMPLETED.
     */
    public boolean isFullyFunded() {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return true;
        }
        return targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0
                && collectedAmount != null && collectedAmount.compareTo(targetAmount) >= 0;
    }

    /**
     * Checks if the campaign is currently open for demo contributions.
     */
    public boolean isAcceptingContributions() {
        return "APPROVED".equalsIgnoreCase(status) && !isFullyFunded();
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCollectedAmount() {
        return collectedAmount;
    }

    public void setCollectedAmount(BigDecimal collectedAmount) {
        this.collectedAmount = collectedAmount;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campaign campaign = (Campaign) o;
        return Objects.equals(id, campaign.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
