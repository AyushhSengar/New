package com.crowdfund.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.security.AuthUser;
import com.crowdfund.util.FirestoreSupport;
import com.crowdfund.util.Money;
import com.crowdfund.util.Text;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;

@Service
public class ContributionService {

    private final Firestore db;

    public ContributionService(Firestore db) {
        this.db = db;
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
