package com.crowdfund.controller;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crowdfund.dto.AdminStatsResponse;
import com.crowdfund.dto.ContributorStatsResponse;
import com.crowdfund.dto.CreatorStatsResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.service.AnalyticsService;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private AnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(analyticsService);
    }

    @Test
    void testGetAdminStats() {
        AuthUser admin = new AuthUser("admin_id", "admin@test.com", "Admin", "ADMIN");
        AdminStatsResponse stats = new AdminStatsResponse(1, 2, 0, 1, 4, new BigDecimal("1500"), 5, 1, 2, 2);
        when(analyticsService.getAdminStats(admin)).thenReturn(stats);

        AdminStatsResponse res = controller.getAdminStats(admin);
        assertNotNull(res);
        assertEquals(4, res.totalCampaigns());
        assertEquals(new BigDecimal("1500"), res.totalRaised());
    }

    @Test
    void testGetCreatorStats() {
        AuthUser creator = new AuthUser("cr_id", "creator@test.com", "Creator", "CREATOR");
        CreatorStatsResponse stats = new CreatorStatsResponse(3, 1, 1, 1, 0, new BigDecimal("800"), new BigDecimal("2000"));
        when(analyticsService.getCreatorStats(creator)).thenReturn(stats);

        CreatorStatsResponse res = controller.getCreatorStats(creator);
        assertNotNull(res);
        assertEquals(3, res.totalCampaigns());
        assertEquals(new BigDecimal("800"), res.totalRaised());
    }

    @Test
    void testGetContributorStats() {
        AuthUser contributor = new AuthUser("usr_id", "donor@test.com", "Donor", "CONTRIBUTOR");
        ContributorStatsResponse stats = new ContributorStatsResponse(2, 4, 2, new BigDecimal("350"));
        when(analyticsService.getContributorStats(contributor)).thenReturn(stats);

        ContributorStatsResponse res = controller.getContributorStats(contributor);
        assertNotNull(res);
        assertEquals(4, res.totalContributionsCount());
        assertEquals(new BigDecimal("350"), res.totalContributedAmount());
    }
}
