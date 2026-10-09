package com.crowdfund.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.crowdfund.dto.AdminStatsResponse;
import com.crowdfund.dto.ContributorStatsResponse;
import com.crowdfund.dto.CreatorStatsResponse;
import com.crowdfund.model.Campaign;
import com.crowdfund.model.Donation;
import com.crowdfund.model.UserSummary;
import com.crowdfund.security.AuthUser;
import com.crowdfund.util.FirestoreSupport;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;

/**
 * Service responsible for computing platform, creator, and contributor analytics.
 * Encapsulates all statistical aggregation rules within Java.
 */
@Service
public class AnalyticsService {

    private final Firestore db;

    public AnalyticsService(Firestore db) {
        this.db = db;
    }

    /**
     * ADMIN only: Computes comprehensive platform metrics.
     */
    public AdminStatsResponse getAdminStats(AuthUser caller) {
        caller.requireRole("ADMIN");

        List<QueryDocumentSnapshot> campaignDocs = FirestoreSupport.await(db.collection("campaigns").get()).getDocuments();
        List<QueryDocumentSnapshot> userDocs = FirestoreSupport.await(db.collection("users").get()).getDocuments();

        List<Campaign> campaigns = campaignDocs.stream().map(doc -> {
            Campaign c = new Campaign();
            c.setId(doc.getId());
            c.setTitle(doc.getString("title"));
            c.setStatus(doc.getString("status"));
            Double target = doc.getDouble("targetAmount");
            Double collected = doc.getDouble("collectedAmount");
            c.setTargetAmount(target != null ? BigDecimal.valueOf(target) : BigDecimal.ZERO);
            c.setCollectedAmount(collected != null ? BigDecimal.valueOf(collected) : BigDecimal.ZERO);
            return c;
        }).collect(Collectors.toList());

        List<UserSummary> users = userDocs.stream().map(doc -> {
            UserSummary u = new UserSummary();
            u.setUid(doc.getId());
            u.setName(doc.getString("name"));
            u.setEmail(doc.getString("email"));
            u.setRole(doc.getString("ROLE"));
            return u;
        }).collect(Collectors.toList());

        return computeAdminStats(campaigns, users);
    }

    /**
     * CREATOR only: Computes metrics for the caller's campaigns.
     */
    public CreatorStatsResponse getCreatorStats(AuthUser caller) {
        caller.requireRole("CREATOR");

        List<QueryDocumentSnapshot> campaignDocs = FirestoreSupport.await(
                db.collection("campaigns")
                        .whereEqualTo("creatorId", caller.uid())
                        .get()
        ).getDocuments();

        List<Campaign> campaigns = campaignDocs.stream().map(doc -> {
            Campaign c = new Campaign();
            c.setId(doc.getId());
            c.setTitle(doc.getString("title"));
            c.setCreatorId(doc.getString("creatorId"));
            c.setStatus(doc.getString("status"));
            Double target = doc.getDouble("targetAmount");
            Double collected = doc.getDouble("collectedAmount");
            c.setTargetAmount(target != null ? BigDecimal.valueOf(target) : BigDecimal.ZERO);
            c.setCollectedAmount(collected != null ? BigDecimal.valueOf(collected) : BigDecimal.ZERO);
            return c;
        }).collect(Collectors.toList());

        return computeCreatorStats(campaigns, caller.uid());
    }

    /**
     * CONTRIBUTOR only: Computes metrics for the contributor's activity.
     */
    public ContributorStatsResponse getContributorStats(AuthUser caller) {
        caller.requireRole("CONTRIBUTOR");

        List<QueryDocumentSnapshot> campaignDocs = FirestoreSupport.await(
                db.collection("campaigns")
                        .whereIn("status", List.of("APPROVED", "COMPLETED"))
                        .get()
        ).getDocuments();

        List<QueryDocumentSnapshot> donationDocs = FirestoreSupport.await(
                db.collection("donations")
                        .whereEqualTo("contributorId", caller.uid())
                        .get()
        ).getDocuments();

        List<Campaign> campaigns = campaignDocs.stream().map(doc -> {
            Campaign c = new Campaign();
            c.setId(doc.getId());
            c.setStatus(doc.getString("status"));
            Double target = doc.getDouble("targetAmount");
            Double collected = doc.getDouble("collectedAmount");
            c.setTargetAmount(target != null ? BigDecimal.valueOf(target) : BigDecimal.ZERO);
            c.setCollectedAmount(collected != null ? BigDecimal.valueOf(collected) : BigDecimal.ZERO);
            return c;
        }).collect(Collectors.toList());

        List<Donation> donations = donationDocs.stream().map(doc -> {
            Donation d = new Donation();
            d.setId(doc.getId());
            d.setCampaignId(doc.getString("campaignId"));
            d.setContributorId(doc.getString("contributorId"));
            Double amount = doc.getDouble("amount");
            d.setAmount(amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO);
            return d;
        }).collect(Collectors.toList());

        return computeContributorStats(campaigns, donations);
    }

    // --- Pure Domain Aggregation Calculations (Directly Unit Testable) ---

    public AdminStatsResponse computeAdminStats(List<Campaign> campaigns, List<UserSummary> users) {
        long pending = 0;
        long approved = 0;
        long rejected = 0;
        long completed = 0;
        BigDecimal totalRaised = BigDecimal.ZERO;

        for (Campaign c : campaigns) {
            String status = c.getStatus() == null ? "PENDING" : c.getStatus().toUpperCase();
            switch (status) {
                case "APPROVED" -> approved++;
                case "REJECTED" -> rejected++;
                case "COMPLETED" -> completed++;
                default -> pending++;
            }
            if (c.getCollectedAmount() != null) {
                totalRaised = totalRaised.add(c.getCollectedAmount());
            }
        }

        long adminCount = 0;
        long creatorCount = 0;
        long contributorCount = 0;

        for (UserSummary u : users) {
            String role = u.getRole() == null ? "" : u.getRole().toUpperCase();
            switch (role) {
                case "ADMIN" -> adminCount++;
                case "CREATOR" -> creatorCount++;
                case "CONTRIBUTOR" -> contributorCount++;
            }
        }

        return new AdminStatsResponse(
                pending,
                approved,
                rejected,
                completed,
                campaigns.size(),
                totalRaised,
                users.size(),
                adminCount,
                creatorCount,
                contributorCount
        );
    }

    public CreatorStatsResponse computeCreatorStats(List<Campaign> campaigns, String creatorId) {
        List<Campaign> own = campaigns.stream()
                .filter(c -> creatorId == null || creatorId.equals(c.getCreatorId()) || c.getCreatorId() == null)
                .toList();

        long pending = 0;
        long live = 0;
        long completed = 0;
        long rejected = 0;
        BigDecimal totalRaised = BigDecimal.ZERO;
        BigDecimal totalTarget = BigDecimal.ZERO;

        for (Campaign c : own) {
            String status = c.getStatus() == null ? "PENDING" : c.getStatus().toUpperCase();
            switch (status) {
                case "APPROVED" -> live++;
                case "COMPLETED" -> completed++;
                case "REJECTED" -> rejected++;
                default -> pending++;
            }
            if (c.getCollectedAmount() != null) {
                totalRaised = totalRaised.add(c.getCollectedAmount());
            }
            if (c.getTargetAmount() != null) {
                totalTarget = totalTarget.add(c.getTargetAmount());
            }
        }

        return new CreatorStatsResponse(
                own.size(),
                pending,
                live,
                completed,
                rejected,
                totalRaised,
                totalTarget
        );
    }

    public ContributorStatsResponse computeContributorStats(List<Campaign> campaigns, List<Donation> userDonations) {
        long openCount = campaigns.stream()
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()) && !c.isFullyFunded())
                .count();

        long totalContributions = userDonations.size();
        Set<String> uniqueCampaigns = userDonations.stream()
                .map(Donation::getCampaignId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        BigDecimal totalContributed = userDonations.stream()
                .map(Donation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ContributorStatsResponse(
                openCount,
                totalContributions,
                uniqueCampaigns.size(),
                totalContributed
        );
    }
}
