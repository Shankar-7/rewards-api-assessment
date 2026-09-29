package com.retailer.rewards.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;

/**
 * The inclusive date range that rewards are calculated for.
 */
public record RewardPeriod(LocalDate startDate, LocalDate endDate) {

    /**
     * Every calendar month touched by this period, in order.
     * Used so that months with no purchases still appear with 0 points.
     */
    public List<YearMonth> months() {
        YearMonth firstMonth = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(endDate);
        return Stream.iterate(firstMonth, month -> !month.isAfter(lastMonth), month -> month.plusMonths(1))
                .toList();
    }
}
