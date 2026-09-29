package com.retailer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long transactionId,
        Long customerId,
        String customerName,
        BigDecimal amount,
        LocalDate transactionDate,
        long rewardPoints) {
}
