package com.retailer.rewards.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Applies the reward rule to a single purchase:
 * 2 points for every whole dollar over $100, plus 1 point for every whole dollar between $50 and $100.
 * Example: $120 = 2 x 20 + 1 x 50 = 90 points.
 * Cents are ignored, so $100.99 is treated as $100.
 */
@Component
public class RewardPointsCalculator {

    private static final long LOWER_THRESHOLD_DOLLARS = 50;
    private static final long UPPER_THRESHOLD_DOLLARS = 100;
    private static final long POINTS_PER_DOLLAR_OVER_UPPER = 2;

    public long calculatePoints(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        long wholeDollars = amount.setScale(0, RoundingMode.DOWN).longValue();

        long points = 0;
        if (wholeDollars > UPPER_THRESHOLD_DOLLARS) {
            points += POINTS_PER_DOLLAR_OVER_UPPER * (wholeDollars - UPPER_THRESHOLD_DOLLARS);
        }
        if (wholeDollars > LOWER_THRESHOLD_DOLLARS) {
            points += Math.min(wholeDollars, UPPER_THRESHOLD_DOLLARS) - LOWER_THRESHOLD_DOLLARS;
        }
        return points;
    }
}
