package com.crowdfund.dto;

/**
 * Filter and query criteria for retrieving campaigns.
 */
public record CampaignFilterCriteria(
        String search,
        String status,
        String creatorId,
        String sortBy,
        String sortDirection
) {
    public CampaignFilterCriteria {
        search = (search == null) ? "" : search.trim();
        status = (status == null) ? "" : status.trim().toUpperCase();
        creatorId = (creatorId == null) ? "" : creatorId.trim();
        sortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        sortDirection = (sortDirection == null || sortDirection.isBlank()) ? "desc" : sortDirection.trim().toLowerCase();
    }

    public static CampaignFilterCriteria empty() {
        return new CampaignFilterCriteria("", "", "", "createdAt", "desc");
    }
}
