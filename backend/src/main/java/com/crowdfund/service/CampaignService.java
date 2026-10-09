package com.crowdfund.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
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
public class CampaignService {

    private final Firestore db;

    public CampaignService(Firestore db) {
        this.db = db;
    }

    /** CREATOR only. Status, collected amount and creator are always set by the server. */
    public Map<String, Object> create(AuthUser caller, CreateCampaignRequest request) {

        caller.requireRole("CREATOR");

        String title = Text.require(request.title(), "Campaign title", 120);
        String description = Text.require(request.description(), "Campaign description", 2000);
        BigDecimal target = Money.parsePositive(request.targetAmount(), "Target amount");

        DocumentReference ref = db.collection("campaigns").document();

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", description);
        data.put("targetAmount", target.doubleValue());
        data.put("collectedAmount", 0.0);
        data.put("creatorId", caller.uid());
        data.put("creatorName", caller.displayName());
        data.put("status", "PENDING");
        data.put("rejectionReason", "");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSupport.await(ref.set(data));

        return Map.of("id", ref.getId(), "status", "PENDING");
    }

    /** ADMIN only. A campaign can be reviewed only while it is PENDING. */
    public Map<String, Object> approve(AuthUser caller, String campaignId) {

        caller.requireRole("ADMIN");

        review(campaignId, "APPROVED", "");

        return Map.of("id", campaignId, "status", "APPROVED");
    }

    /** ADMIN only. A rejection reason is mandatory. */
    public Map<String, Object> reject(AuthUser caller, String campaignId, ReasonRequest request) {

        caller.requireRole("ADMIN");

        String reason = Text.require(request.reason(), "Rejection reason", 500);

        review(campaignId, "REJECTED", reason);

        return Map.of("id", campaignId, "status", "REJECTED");
    }

    private void review(String campaignId, String newStatus, String reason) {

        Text.requireId(campaignId);

        DocumentReference ref = db.collection("campaigns").document(campaignId);

        FirestoreSupport.transaction(db, transaction -> {

            DocumentSnapshot snapshot = transaction.get(ref).get();

            if (!snapshot.exists()) {
                throw new AppException(HttpStatus.NOT_FOUND, "Campaign not found.");
            }

            String status = String.valueOf(snapshot.getString("status")).toUpperCase(Locale.ROOT);

            if (!status.equals("PENDING")) {
                throw new AppException(HttpStatus.CONFLICT,
                        "Only pending campaigns can be approved or rejected. "
                                + "This campaign is already " + status + ".");
            }

            Map<String, Object> update = new HashMap<>();
            update.put("status", newStatus);
            update.put("rejectionReason", reason);
            update.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(ref, update);

            return null;
        });
    }
}
