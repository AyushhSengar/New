package com.crowdfund.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.security.AuthUser;
import com.google.cloud.firestore.Firestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
