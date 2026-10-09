package com.crowdfund.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.dto.CreateCampaignRequest;
import com.crowdfund.dto.ReasonRequest;
import com.crowdfund.security.AuthUser;
import com.crowdfund.security.FirebaseAuthFilter;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final ContributionService contributionService;

    public CampaignController(
            CampaignService campaignService,
            ContributionService contributionService) {

        this.campaignService = campaignService;
        this.contributionService = contributionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @RequestBody CreateCampaignRequest request) {

        return campaignService.create(user, request);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @PathVariable String id) {

        return campaignService.approve(user, id);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @PathVariable String id,
            @RequestBody ReasonRequest request) {

        return campaignService.reject(user, id, request);
    }

    @PostMapping("/{id}/contributions")
    public Map<String, Object> contribute(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @PathVariable String id,
            @RequestBody AmountRequest request) {

        return contributionService.contribute(user, id, request);
    }
}
