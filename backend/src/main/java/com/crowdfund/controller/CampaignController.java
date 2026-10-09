package com.crowdfund.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.crowdfund.dto.AmountRequest;
import com.crowdfund.dto.CampaignFilterCriteria;
import com.crowdfund.dto.CampaignResponse;
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

    /**
     * Lists campaigns with server-side role scoping, search query filtering and sorting.
     */
    @GetMapping
    public List<CampaignResponse> list(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String creatorId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        CampaignFilterCriteria criteria = new CampaignFilterCriteria(search, status, creatorId, sortBy, sortDirection);
        return campaignService.listCampaigns(user, criteria);
    }

    /**
     * Retrieves a single campaign with calculated progress and role access checks.
     */
    @GetMapping("/{id}")
    public CampaignResponse getById(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @PathVariable String id) {

        return campaignService.getCampaignById(user, id);
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
