package com.crowdfund.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.dto.ContributionHistoryResponse;
import com.crowdfund.exception.AppException;
import com.crowdfund.model.Campaign;
import com.crowdfund.model.Donation;
import com.crowdfund.security.AuthUser;
import com.crowdfund.util.FirestoreSupport;
import com.crowdfund.util.Money;
import com.crowdfund.util.Text;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;

@Service
public class ContributionService {

    private final Firestore db;

    public ContributionService(Firestore db) {
        this.db = db;
    }

    /**
     * CONTRIBUTOR only. Retrieves the contributor's simulated donation history enriched with campaign progress.
     */
    public List<ContributionHistoryResponse> getContributorHistory(AuthUser caller) {
        caller.requireRole("CONTRIBUTOR");

        List<QueryDocumentSnapshot> donationDocs = FirestoreSupport.await(
                db.collection("donations")
                        .whereEqualTo("contributorId", caller.uid())
                        .get()
        ).getDocuments();

        List<Donation> donations = donationDocs.stream().map(doc -> {
            Donation d = new Donation();
            d.setId(doc.getId());
            d.setCampaignId(doc.getString("campaignId"));
            d.setCampaignTitle(doc.getString("campaignTitle"));
            d.setContributorId(doc.getString("contributorId"));
            d.setContributorName(doc.getString("contributorName"));
            d.setContributorEmail(doc.getString("contributorEmail"));
            Double amt = doc.getDouble("amount");
            d.setAmount(amt != null ? BigDecimal.valueOf(amt) : BigDecimal.ZERO);
            Boolean sim = doc.getBoolean("simulated");
            d.setSimulated(sim != null ? sim : true);
            Timestamp ts = doc.getTimestamp("createdAt");
            if (ts != null) {
                d.setCreatedAt(ts.toDate().toInstant());
            }
            return d;
        }).collect(Collectors.toList());

        // Fetch campaigns to enrich
        List<String> campaignIds = donations.stream()
                .map(Donation::getCampaignId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Campaign> campaignMap = new HashMap<>();
        for (String cId : campaignIds) {
            try {
                DocumentSnapshot cDoc = FirestoreSupport.await(db.collection("campaigns").document(cId).get());
                if (cDoc.exists()) {
                    Campaign c = new Campaign();
                    c.setId(cDoc.getId());
                    c.setTitle(cDoc.getString("title"));
                    c.setStatus(cDoc.getString("status"));
                    Double target = cDoc.getDouble("targetAmount");
                    Double collected = cDoc.getDouble("collectedAmount");
                    c.setTargetAmount(target != null ? BigDecimal.valueOf(target) : BigDecimal.ZERO);
                    c.setCollectedAmount(collected != null ? BigDecimal.valueOf(collected) : BigDecimal.ZERO);
                    campaignMap.put(cId, c);
                }
            } catch (Exception ignored) {
                // If campaign document is missing, fallback cleanly
            }
        }

        return enrichDonations(donations, campaignMap);
    }

    public List<ContributionHistoryResponse> enrichDonations(List<Donation> donations, Map<String, Campaign> campaignMap) {
        List<ContributionHistoryResponse> results = new ArrayList<>();
        if (donations == null) {
            return results;
        }

        for (Donation d : donations) {
            Campaign c = campaignMap != null ? campaignMap.get(d.getCampaignId()) : null;
            String status = c != null ? c.getStatus() : "APPROVED";
            BigDecimal target = c != null ? c.getTargetAmount() : BigDecimal.ZERO;
            BigDecimal collected = c != null ? c.getCollectedAmount() : BigDecimal.ZERO;
            int percentage = c != null ? c.calculatePercentage() : 0;
            String title = d.getCampaignTitle();
            if (title == null || title.isBlank()) {
                title = c != null && c.getTitle() != null ? c.getTitle() : "Campaign";
            }

            results.add(new ContributionHistoryResponse(
                    d.getId(),
                    d.getCampaignId(),
                    title,
                    d.getAmount(),
                    d.isSimulated(),
                    d.getCreatedAt(),
                    status,
                    target,
                    collected,
                    percentage
            ));
        }

        results.sort((a, b) -> {
            Instant timeA = a.createdAt() != null ? a.createdAt() : Instant.EPOCH;
            Instant timeB = b.createdAt() != null ? b.createdAt() : Instant.EPOCH;
            return timeB.compareTo(timeA);
        });

        return results;
    }

    /**
     * CONTRIBUTOR only. In ONE atomic transaction this reads the campaign,
     * checks the rules, updates the collected total and writes the donation record,
     * so concurrent contributions can never corrupt the total.
     * Note: this records a simulated contribution; no real payment is processed.
     */
    public Map<String, Object> contribute(AuthUser caller, String campaignId, AmountRequest request) {

        caller.requireRole("CONTRIBUTOR");

        Text.requireId(campaignId);

        BigDecimal amount = Money.parsePositive(request.amount(), "Contribution amount");

        DocumentReference campaignRef = db.collection("campaigns").document(campaignId);
        DocumentReference donationRef = db.collection("donations").document();

        return FirestoreSupport.transaction(db, transaction -> {

            DocumentSnapshot campaign = transaction.get(campaignRef).get();

            if (!campaign.exists()) {
                throw new AppException(HttpStatus.NOT_FOUND, "Campaign not found.");
            }

            String status = String.valueOf(campaign.getString("status")).toUpperCase(Locale.ROOT);

            if (status.equals("COMPLETED")) {
                throw new AppException(HttpStatus.CONFLICT,
                        "This campaign has already reached its goal.");
            }

            if (!status.equals("APPROVED")) {
                throw new AppException(HttpStatus.CONFLICT,
                        "This campaign is not accepting contributions.");
            }

            BigDecimal target = Money.fromFirestore(campaign.get("targetAmount"));
            BigDecimal collected = Money.fromFirestore(campaign.get("collectedAmount"));
            BigDecimal remaining = target.subtract(collected);

            if (remaining.signum() <= 0) {
                throw new AppException(HttpStatus.CONFLICT,
                        "This campaign has already reached its goal.");
            }

            if (amount.compareTo(remaining) > 0) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Maximum contribution is " + remaining.toPlainString() + ".");
            }

            BigDecimal newCollected = collected.add(amount);
            String newStatus = newCollected.compareTo(target) >= 0 ? "COMPLETED" : "APPROVED";

            Map<String, Object> campaignUpdate = new HashMap<>();
            campaignUpdate.put("collectedAmount", newCollected.doubleValue());
            campaignUpdate.put("status", newStatus);
            campaignUpdate.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(campaignRef, campaignUpdate);

            Map<String, Object> donation = new HashMap<>();
            donation.put("contributorId", caller.uid());
            donation.put("contributorEmail", caller.email() == null ? "" : caller.email());
            donation.put("contributorName", caller.displayName());
            donation.put("campaignId", campaignId);
            donation.put("campaignTitle", String.valueOf(campaign.getString("title")));
            donation.put("amount", amount.doubleValue());
            donation.put("simulated", true);
            donation.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(donationRef, donation);

            Map<String, Object> result = new HashMap<>();
            result.put("donationId", donationRef.getId());
            result.put("collectedAmount", newCollected.doubleValue());
            result.put("status", newStatus);

            return result;
        });
    }
}
