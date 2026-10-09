package com.crowdfund.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.crowdfund.dto.CampaignFilterCriteria;
import com.crowdfund.dto.CampaignResponse;
import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.model.Campaign;
import com.crowdfund.security.AuthUser;
import com.crowdfund.util.FirestoreSupport;
import com.crowdfund.util.Money;
import com.crowdfund.util.Text;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;

@Service
public class CampaignService {

    private final Firestore db;

    public CampaignService(Firestore db) {
        this.db = db;
    }

    /**
     * Lists campaigns scoped by the caller's role with search, filtering, and priority sorting.
     */
    public List<CampaignResponse> listCampaigns(AuthUser caller, CampaignFilterCriteria criteria) {
        String role = caller.role() == null ? "" : caller.role().toUpperCase(Locale.ROOT);
        Query query = db.collection("campaigns");

        if ("CREATOR".equals(role)) {
            query = query.whereEqualTo("creatorId", caller.uid());
        } else if ("CONTRIBUTOR".equals(role)) {
            query = query.whereIn("status", List.of("APPROVED", "COMPLETED"));
        }

        List<QueryDocumentSnapshot> docs = FirestoreSupport.await(query.get()).getDocuments();
        List<Campaign> list = docs.stream().map(this::mapDocToCampaign).collect(Collectors.toList());

        // Apply Java-side filtering & sorting rules
        return filterAndSortCampaigns(list, criteria, role).stream()
                .map(CampaignResponse::fromModel)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single campaign with domain validation and security rules.
     */
    public CampaignResponse getCampaignById(AuthUser caller, String campaignId) {
        Text.requireId(campaignId);

        DocumentReference ref = db.collection("campaigns").document(campaignId);
        DocumentSnapshot doc = FirestoreSupport.await(ref.get());

        if (!doc.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Campaign not found.");
        }

        Campaign campaign = mapDocToCampaign(doc);
        String role = caller.role() == null ? "" : caller.role().toUpperCase(Locale.ROOT);

        // Security check
        if ("CREATOR".equals(role) && !caller.uid().equals(campaign.getCreatorId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to view this campaign.");
        }
        if ("CONTRIBUTOR".equals(role) && !"APPROVED".equalsIgnoreCase(campaign.getStatus())
                && !"COMPLETED".equalsIgnoreCase(campaign.getStatus())) {
            throw new AppException(HttpStatus.FORBIDDEN, "This campaign is not publicly available.");
        }

        return CampaignResponse.fromModel(campaign);
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

    // --- Domain Helpers & Calculation Utilities ---

    public List<Campaign> filterAndSortCampaigns(List<Campaign> campaigns, CampaignFilterCriteria criteria, String role) {
        if (campaigns == null) {
            return List.of();
        }

        String search = criteria != null && criteria.search() != null ? criteria.search().toLowerCase().trim() : "";
        String statusFilter = criteria != null && criteria.status() != null ? criteria.status().trim().toUpperCase() : "";

        List<Campaign> filtered = new ArrayList<>();
        for (Campaign c : campaigns) {
            if (!statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase(c.getStatus())) {
                continue;
            }
            if (!search.isEmpty()) {
                boolean matchesTitle = c.getTitle() != null && c.getTitle().toLowerCase().contains(search);
                boolean matchesDesc = c.getDescription() != null && c.getDescription().toLowerCase().contains(search);
                boolean matchesCreator = c.getCreatorName() != null && c.getCreatorName().toLowerCase().contains(search);
                if (!matchesTitle && !matchesDesc && !matchesCreator) {
                    continue;
                }
            }
            filtered.add(c);
        }

        // Sorting: ADMIN has PENDING first priority; otherwise by creation date descending
        boolean pendingFirst = "ADMIN".equalsIgnoreCase(role);
        filtered.sort((a, b) -> {
            if (pendingFirst) {
                int rankA = "PENDING".equalsIgnoreCase(a.getStatus()) ? 0 : ("COMPLETED".equalsIgnoreCase(a.getStatus()) ? 2 : 1);
                int rankB = "PENDING".equalsIgnoreCase(b.getStatus()) ? 0 : ("COMPLETED".equalsIgnoreCase(b.getStatus()) ? 2 : 1);
                if (rankA != rankB) {
                    return Integer.compare(rankA, rankB);
                }
            }
            Instant timeA = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
            Instant timeB = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
            return timeB.compareTo(timeA);
        });

        return filtered;
    }

    private Campaign mapDocToCampaign(DocumentSnapshot doc) {
        Campaign c = new Campaign();
        c.setId(doc.getId());
        c.setTitle(doc.getString("title"));
        c.setDescription(doc.getString("description"));
        Double target = doc.getDouble("targetAmount");
        Double collected = doc.getDouble("collectedAmount");
        c.setTargetAmount(target != null ? BigDecimal.valueOf(target) : BigDecimal.ZERO);
        c.setCollectedAmount(collected != null ? BigDecimal.valueOf(collected) : BigDecimal.ZERO);
        c.setCreatorId(doc.getString("creatorId"));
        c.setCreatorName(doc.getString("creatorName"));
        c.setStatus(doc.getString("status"));
        c.setRejectionReason(doc.getString("rejectionReason"));

        Timestamp created = doc.getTimestamp("createdAt");
        if (created != null) {
            c.setCreatedAt(created.toDate().toInstant());
        }
        Timestamp updated = doc.getTimestamp("updatedAt");
        if (updated != null) {
            c.setUpdatedAt(updated.toDate().toInstant());
        }

        return c;
    }
}
