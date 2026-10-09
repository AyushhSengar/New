package com.crowdfund.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.crowdfund.dto.CampaignFilterCriteria;
import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.model.Campaign;
import com.crowdfund.security.AuthUser;
import com.google.cloud.firestore.Firestore;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private Firestore db;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    @DisplayName("create blocks non-CREATOR roles")
    void testCreateRoleRestriction() {
        AuthUser contributor = new AuthUser("uid1", "donor@test.com", "Donor", "CONTRIBUTOR");
        CreateCampaignRequest req = new CreateCampaignRequest("Title", "Description", new BigDecimal("1000.00"));

        AppException ex = assertThrows(AppException.class,
                () -> campaignService.create(contributor, req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("create validates title, description and target amount")
    void testCreateValidation() {
        AuthUser creator = new AuthUser("uid2", "creator@test.com", "Creator", "CREATOR");

        CreateCampaignRequest emptyTitle = new CreateCampaignRequest("", "Description", new BigDecimal("1000.00"));
        AppException exTitle = assertThrows(AppException.class,
                () -> campaignService.create(creator, emptyTitle));
        assertEquals(HttpStatus.BAD_REQUEST, exTitle.getStatus());

        CreateCampaignRequest emptyDesc = new CreateCampaignRequest("Title", "", new BigDecimal("1000.00"));
        AppException exDesc = assertThrows(AppException.class,
                () -> campaignService.create(creator, emptyDesc));
        assertEquals(HttpStatus.BAD_REQUEST, exDesc.getStatus());

        CreateCampaignRequest zeroTarget = new CreateCampaignRequest("Title", "Description", BigDecimal.ZERO);
        AppException exTarget = assertThrows(AppException.class,
                () -> campaignService.create(creator, zeroTarget));
        assertEquals(HttpStatus.BAD_REQUEST, exTarget.getStatus());
    }

    @Test
    @DisplayName("approve and reject block non-ADMIN roles")
    void testReviewRoleRestriction() {
        AuthUser creator = new AuthUser("uid2", "creator@test.com", "Creator", "CREATOR");

        AppException exApprove = assertThrows(AppException.class,
                () -> campaignService.approve(creator, "camp123"));
        assertEquals(HttpStatus.FORBIDDEN, exApprove.getStatus());

        AppException exReject = assertThrows(AppException.class,
                () -> campaignService.reject(creator, "camp123", new ReasonRequest("Reason")));
        assertEquals(HttpStatus.FORBIDDEN, exReject.getStatus());
    }

    @Test
    @DisplayName("reject requires a non-empty reason")
    void testRejectReasonValidation() {
        AuthUser admin = new AuthUser("uid3", "admin@test.com", "Admin", "ADMIN");

        AppException exEmpty = assertThrows(AppException.class,
                () -> campaignService.reject(admin, "camp123", new ReasonRequest("")));
        assertEquals(HttpStatus.BAD_REQUEST, exEmpty.getStatus());

        AppException exNull = assertThrows(AppException.class,
                () -> campaignService.reject(admin, "camp123", new ReasonRequest(null)));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatus());
    }

    @Test
    @DisplayName("filterAndSortCampaigns filters by search, status and sorts with Admin pending priority")
    void testFilterAndSortCampaigns() {
        Instant now = Instant.now();
        Campaign c1 = new Campaign("1", "Clean Oceans", "Marine cleanup", new BigDecimal("1000"), BigDecimal.ZERO, "u1", "OceanOrg", "APPROVED", "", now.minusSeconds(100), now);
        Campaign c2 = new Campaign("2", "Tree Planting", "Reforestation", new BigDecimal("2000"), BigDecimal.ZERO, "u2", "GreenEarth", "PENDING", "", now.minusSeconds(50), now);
        Campaign c3 = new Campaign("3", "Solar Energy", "Solar panels", new BigDecimal("3000"), new BigDecimal("3000"), "u3", "SunPower", "COMPLETED", "", now, now);

        List<Campaign> list = List.of(c1, c2, c3);

        // ADMIN view: pending campaigns come first
        CampaignFilterCriteria emptyCriteria = CampaignFilterCriteria.empty();
        List<Campaign> adminSorted = campaignService.filterAndSortCampaigns(list, emptyCriteria, "ADMIN");
        assertEquals("2", adminSorted.get(0).getId()); // PENDING first
        assertEquals("1", adminSorted.get(1).getId()); // APPROVED next
        assertEquals("3", adminSorted.get(2).getId()); // COMPLETED last

        // Search filtering: "planting"
        CampaignFilterCriteria searchCriteria = new CampaignFilterCriteria("planting", "", "", "createdAt", "desc");
        List<Campaign> searched = campaignService.filterAndSortCampaigns(list, searchCriteria, "CONTRIBUTOR");
        assertEquals(1, searched.size());
        assertEquals("2", searched.get(0).getId());

        // Status filtering: "APPROVED"
        CampaignFilterCriteria statusCriteria = new CampaignFilterCriteria("", "APPROVED", "", "createdAt", "desc");
        List<Campaign> statusFiltered = campaignService.filterAndSortCampaigns(list, statusCriteria, "CONTRIBUTOR");
        assertEquals(1, statusFiltered.size());
        assertEquals("1", statusFiltered.get(0).getId());
    }
}
