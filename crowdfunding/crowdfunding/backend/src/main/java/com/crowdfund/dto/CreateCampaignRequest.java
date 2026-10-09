package com.crowdfund.dto;

import java.math.BigDecimal;

public record CreateCampaignRequest(String title, String description, BigDecimal targetAmount) {
}
