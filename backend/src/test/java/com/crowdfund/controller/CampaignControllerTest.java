package com.crowdfund.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.dto.CampaignFilterCriteria;
import com.crowdfund.dto.CampaignResponse;
import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
import com.crowdfund.security.AuthUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @Mock
    private ContributionService contributionService;

    private CampaignController controller;

    @BeforeEach
    void setUp() {
        controller = new CampaignController(campaignService, contributionService);
    }

    @Test
    void testList() {
        AuthUser user = new AuthUser("u1", "user@test.com", "User", "CONTRIBUTOR");
        CampaignResponse item = new CampaignResponse("c1", "Title", "Desc", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"), 0, "cr1", "Creator", "APPROVED", "", false, true, null, null);
        when(campaignService.listCampaigns(user, new CampaignFilterCriteria("search", "APPROVED", null, "createdAt", "desc"))).thenReturn(List.of(item));

        List<CampaignResponse> res = controller.list(user, "search", "APPROVED", null, "createdAt", "desc");
        assertEquals(1, res.size());
        assertEquals("c1", res.get(0).id());
    }

    @Test
    void testGetById() {
        AuthUser user = new AuthUser("u1", "user@test.com", "User", "CONTRIBUTOR");
        CampaignResponse item = new CampaignResponse("c1", "Title", "Desc", new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"), 0, "cr1", "Creator", "APPROVED", "", false, true, null, null);
        when(campaignService.getCampaignById(user, "c1")).thenReturn(item);

        CampaignResponse res = controller.getById(user, "c1");
        assertNotNull(res);
        assertEquals("Title", res.title());
    }

    @Test
    void testCreate() {
        AuthUser creator = new AuthUser("u1", "creator@test.com", "Creator", "CREATOR");
        CreateCampaignRequest req = new CreateCampaignRequest("New Campaign", "Desc", new BigDecimal("500"));
        when(campaignService.create(creator, req)).thenReturn(Map.of("id", "new_c1", "status", "PENDING"));

        Map<String, Object> res = controller.create(creator, req);
        assertEquals("new_c1", res.get("id"));
        assertEquals("PENDING", res.get("status"));
    }

    @Test
    void testApprove() {
        AuthUser admin = new AuthUser("admin_id", "admin@test.com", "Admin", "ADMIN");
        when(campaignService.approve(admin, "c1")).thenReturn(Map.of("id", "c1", "status", "APPROVED"));

        Map<String, Object> res = controller.approve(admin, "c1");
        assertEquals("APPROVED", res.get("status"));
    }

    @Test
    void testReject() {
        AuthUser admin = new AuthUser("admin_id", "admin@test.com", "Admin", "ADMIN");
        ReasonRequest req = new ReasonRequest("Incomplete proposal");
        when(campaignService.reject(admin, "c1", req)).thenReturn(Map.of("id", "c1", "status", "REJECTED"));

        Map<String, Object> res = controller.reject(admin, "c1", req);
        assertEquals("REJECTED", res.get("status"));
    }

    @Test
    void testContribute() {
        AuthUser contributor = new AuthUser("u2", "donor@test.com", "Donor", "CONTRIBUTOR");
        AmountRequest req = new AmountRequest(new BigDecimal("100"));
        when(contributionService.contribute(contributor, "c1", req)).thenReturn(Map.of("status", "APPROVED", "collectedAmount", 100.0));

        Map<String, Object> res = controller.contribute(contributor, "c1", req);
        assertEquals("APPROVED", res.get("status"));
    }
}
