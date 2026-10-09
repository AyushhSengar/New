package com.crowdfund.service;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.crowdfund.dto.AdminStatsResponse;
import com.crowdfund.dto.ContributorStatsResponse;
import com.crowdfund.dto.CreatorStatsResponse;
import com.crowdfund.model.Campaign;
import com.crowdfund.model.Donation;
import com.crowdfund.model.UserSummary;
import com.google.cloud.firestore.Firestore;

class AnalyticsServiceTest {

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        Firestore db = Mockito.mock(Firestore.class);
        service = new AnalyticsService(db);
    }

    @Test
    void testComputeAdminStats() {
        Campaign c1 = new Campaign("1", "Water Project", "Clean water", new BigDecimal("1000"), new BigDecimal("500"), "u1", "Alice", "APPROVED", "", null, null);
        Campaign c2 = new Campaign("2", "School Project", "Education", new BigDecimal("2000"), new BigDecimal("2000"), "u1", "Alice", "COMPLETED", "", null, null);
        Campaign c3 = new Campaign("3", "Solar Project", "Green energy", new BigDecimal("1500"), BigDecimal.ZERO, "u2", "Bob", "PENDING", "", null, null);
        Campaign c4 = new Campaign("4", "Invalid Project", "Bad", new BigDecimal("500"), BigDecimal.ZERO, "u2", "Bob", "REJECTED", "Too vague", null, null);

        UserSummary u1 = new UserSummary("u1", "Alice", "alice@example.com", "CREATOR", null);
        UserSummary u2 = new UserSummary("u2", "Bob", "bob@example.com", "CREATOR", null);
        UserSummary u3 = new UserSummary("u3", "Charlie", "charlie@example.com", "CONTRIBUTOR", null);
        UserSummary u4 = new UserSummary("u4", "Dave Admin", "admin@example.com", "ADMIN", null);

        AdminStatsResponse stats = service.computeAdminStats(List.of(c1, c2, c3, c4), List.of(u1, u2, u3, u4));

        assertEquals(1, stats.pendingCampaigns());
        assertEquals(1, stats.approvedCampaigns());
        assertEquals(1, stats.rejectedCampaigns());
        assertEquals(1, stats.completedCampaigns());
        assertEquals(4, stats.totalCampaigns());
        assertEquals(new BigDecimal("2500"), stats.totalRaised());
        assertEquals(4, stats.totalUsers());
        assertEquals(1, stats.adminCount());
        assertEquals(2, stats.creatorCount());
        assertEquals(1, stats.contributorCount());
    }

    @Test
    void testComputeCreatorStats() {
        Campaign c1 = new Campaign("1", "Camp 1", "Desc", new BigDecimal("1000"), new BigDecimal("300"), "c_123", "Creator", "APPROVED", "", null, null);
        Campaign c2 = new Campaign("2", "Camp 2", "Desc", new BigDecimal("500"), new BigDecimal("500"), "c_123", "Creator", "COMPLETED", "", null, null);
        Campaign c3 = new Campaign("3", "Camp 3", "Desc", new BigDecimal("800"), BigDecimal.ZERO, "c_123", "Creator", "PENDING", "", null, null);
        Campaign c4 = new Campaign("4", "Other Creator", "Desc", new BigDecimal("5000"), new BigDecimal("1000"), "other_user", "Other", "APPROVED", "", null, null);

        CreatorStatsResponse stats = service.computeCreatorStats(List.of(c1, c2, c3, c4), "c_123");

        assertEquals(3, stats.totalCampaigns());
        assertEquals(1, stats.pendingCampaigns());
        assertEquals(1, stats.liveCampaigns());
        assertEquals(1, stats.completedCampaigns());
        assertEquals(0, stats.rejectedCampaigns());
        assertEquals(new BigDecimal("800"), stats.totalRaised());
        assertEquals(new BigDecimal("2300"), stats.totalTarget());
    }

    @Test
    void testComputeContributorStats() {
        Campaign c1 = new Campaign("c1", "Camp 1", "Desc", new BigDecimal("1000"), new BigDecimal("300"), "cr1", "Creator", "APPROVED", "", null, null);
        Campaign c2 = new Campaign("c2", "Camp 2", "Desc", new BigDecimal("500"), new BigDecimal("500"), "cr2", "Creator", "COMPLETED", "", null, null);

        Donation d1 = new Donation("d1", "c1", "Camp 1", "usr1", "User", "u@e.com", new BigDecimal("150"), true, null);
        Donation d2 = new Donation("d2", "c1", "Camp 1", "usr1", "User", "u@e.com", new BigDecimal("50"), true, null);
        Donation d3 = new Donation("d3", "c2", "Camp 2", "usr1", "User", "u@e.com", new BigDecimal("200"), true, null);

        ContributorStatsResponse stats = service.computeContributorStats(List.of(c1, c2), List.of(d1, d2, d3));

        assertEquals(1, stats.openCampaignsCount());
        assertEquals(3, stats.totalContributionsCount());
        assertEquals(2, stats.uniqueCampaignsSupported());
        assertEquals(new BigDecimal("400"), stats.totalContributedAmount());
    }
}
