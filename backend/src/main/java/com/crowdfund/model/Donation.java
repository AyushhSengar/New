package com.crowdfund.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a simulated demo contribution in FundHub.
 */
public class Donation {

    private String id;
    private String campaignId;
    private String campaignTitle;
    private String contributorId;
    private String contributorName;
    private String contributorEmail;
    private BigDecimal amount;
    private boolean simulated;
    private Instant createdAt;

    public Donation() {
        this.amount = BigDecimal.ZERO;
        this.simulated = true;
    }

    public Donation(String id, String campaignId, String campaignTitle,
                    String contributorId, String contributorName, String contributorEmail,
                    BigDecimal amount, boolean simulated, Instant createdAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.campaignTitle = campaignTitle;
        this.contributorId = contributorId;
        this.contributorName = contributorName;
        this.contributorEmail = contributorEmail;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.simulated = simulated;
        this.createdAt = createdAt;
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignTitle() {
        return campaignTitle;
    }

    public void setCampaignTitle(String campaignTitle) {
        this.campaignTitle = campaignTitle;
    }

    public String getContributorId() {
        return contributorId;
    }

    public void setContributorId(String contributorId) {
        this.contributorId = contributorId;
    }

    public String getContributorName() {
        return contributorName;
    }

    public void setContributorName(String contributorName) {
        this.contributorName = contributorName;
    }

    public String getContributorEmail() {
        return contributorEmail;
    }

    public void setContributorEmail(String contributorEmail) {
        this.contributorEmail = contributorEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Donation donation = (Donation) o;
        return Objects.equals(id, donation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
