package com.crowdfund.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crowdfund.dto.ContributionHistoryResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.service.ContributionService;

@ExtendWith(MockitoExtension.class)
class ContributionControllerTest {

    @Mock
    private ContributionService contributionService;

    private ContributionController controller;

    @BeforeEach
    void setUp() {
        controller = new ContributionController(contributionService);
    }

    @Test
    void testMyContributions() {
        AuthUser user = new AuthUser("u1", "donor@test.com", "Donor", "CONTRIBUTOR");
        ContributionHistoryResponse item = new ContributionHistoryResponse(
                "d1", "c1", "Camp Title", new BigDecimal("100"), true, Instant.now(),
                "APPROVED", new BigDecimal("1000"), new BigDecimal("500"), 50
        );

        when(contributionService.getContributorHistory(user)).thenReturn(List.of(item));

        List<ContributionHistoryResponse> res = controller.myContributions(user);
        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("d1", res.get(0).id());
        assertEquals("Camp Title", res.get(0).campaignTitle());
    }
}
