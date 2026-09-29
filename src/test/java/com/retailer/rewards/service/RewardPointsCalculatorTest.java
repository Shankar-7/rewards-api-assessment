package com.retailer.rewards.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RewardPointsCalculatorTest {

    private final RewardPointsCalculator calculator = new RewardPointsCalculator();

    @ParameterizedTest(name = "${0} earns {1} points")
    @CsvSource({
            "0.00,    0",
            "49.99,   0",
            "50.00,   0",
            "50.99,   0",
            "51.00,   1",
            "75.00,   25",
            "99.99,   49",
            "100.00,  50",
            "100.99,  50",
            "101.00,  52",
            "120.00,  90",
            "200.00,  250",
            "1000.00, 1850"
    })
    void calculatesPointsForPurchaseAmount(String amount, long expectedPoints) {
        assertThat(calculator.calculatePoints(new BigDecimal(amount))).isEqualTo(expectedPoints);
    }

    @Test
    void exampleFromRequirements_120DollarPurchaseEarns90Points() {
        assertThat(calculator.calculatePoints(new BigDecimal("120"))).isEqualTo(90);
    }

    @Test
    void purchaseOfExactly100DollarsEarns50Points() {
        assertThat(calculator.calculatePoints(new BigDecimal("100"))).isEqualTo(50);
    }

    @Test
    void negativeAmountEarnsNoPoints() {
        assertThat(calculator.calculatePoints(new BigDecimal("-25.00"))).isZero();
    }

    @Test
    void nullAmountIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> calculator.calculatePoints(null));
    }
}
