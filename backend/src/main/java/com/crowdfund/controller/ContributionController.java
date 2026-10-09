package com.crowdfund.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crowdfund.dto.ContributionHistoryResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.security.FirebaseAuthFilter;
import com.crowdfund.service.ContributionService;

@RestController
@RequestMapping("/api/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    /**
     * CONTRIBUTOR only: Returns enriched donation history.
     */
    @GetMapping("/mine")
    public List<ContributionHistoryResponse> myContributions(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {

        return contributionService.getContributorHistory(user);
    }
}
