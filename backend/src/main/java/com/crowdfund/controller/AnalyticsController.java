package com.crowdfund.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crowdfund.dto.AdminStatsResponse;
import com.crowdfund.dto.ContributorStatsResponse;
import com.crowdfund.dto.CreatorStatsResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.security.FirebaseAuthFilter;
import com.crowdfund.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/admin-stats")
    public AdminStatsResponse getAdminStats(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {
        return analyticsService.getAdminStats(user);
    }

    @GetMapping("/creator-stats")
    public CreatorStatsResponse getCreatorStats(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {
        return analyticsService.getCreatorStats(user);
    }

    @GetMapping("/contributor-stats")
    public ContributorStatsResponse getContributorStats(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {
        return analyticsService.getContributorStats(user);
    }
}
