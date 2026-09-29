package com.retailer.rewards.dto;

import java.time.LocalDate;
import java.util.List;

public record CustomerRewardsResponse(
        Long customerId,
        String customerName,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<MonthlyReward> monthlyRewards,
        long totalPoints) {
}
