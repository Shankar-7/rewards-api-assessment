package com.retailer.rewards.dto;

import java.time.Month;

public record MonthlyReward(int year, Month month, long points) {
}
