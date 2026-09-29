package com.retailer.rewards.service;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyReward;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private static final LocalDate DEFAULT_PERIOD_START = LocalDate.of(2026, 1, 1);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private RewardService rewardService;

    private final Customer alice = new Customer(1L, "Alice");
    private final Customer bob = new Customer(2L, "Bob");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        rewardService = new RewardService(customerRepository, transactionRepository,
                new RewardPointsCalculator(), fixedClock);
    }

    @Test
    void customerRewards_areGroupedByMonthAndTotalled() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(1L, DEFAULT_PERIOD_START, TODAY))
                .thenReturn(List.of(
                        purchase(alice, "120.00", 2026, 1, 5),   // 90
                        purchase(alice, "75.00", 2026, 1, 20),   // 25
                        purchase(alice, "100.00", 2026, 2, 10),  // 50
                        purchase(alice, "200.00", 2026, 3, 1))); // 250

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L, null, null);

        assertThat(rewards.customerId()).isEqualTo(1L);
        assertThat(rewards.customerName()).isEqualTo("Alice");
        assertThat(rewards.monthlyRewards()).containsExactly(
                new MonthlyReward(2026, Month.JANUARY, 115),
                new MonthlyReward(2026, Month.FEBRUARY, 50),
                new MonthlyReward(2026, Month.MARCH, 250));
        assertThat(rewards.totalPoints()).isEqualTo(415);
    }

    @Test
    void defaultPeriod_isCurrentMonthAndTwoPreviousMonths() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(1L, DEFAULT_PERIOD_START, TODAY))
                .thenReturn(List.of());

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L, null, null);

        assertThat(rewards.periodStart()).isEqualTo(DEFAULT_PERIOD_START);
        assertThat(rewards.periodEnd()).isEqualTo(TODAY);
    }

    @Test
    void monthWithNoPurchases_isReturnedWithZeroPoints() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(1L, DEFAULT_PERIOD_START, TODAY))
                .thenReturn(List.of(purchase(alice, "120.00", 2026, 3, 2)));

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L, null, null);

        assertThat(rewards.monthlyRewards()).containsExactly(
                new MonthlyReward(2026, Month.JANUARY, 0),
                new MonthlyReward(2026, Month.FEBRUARY, 0),
                new MonthlyReward(2026, Month.MARCH, 90));
        assertThat(rewards.totalPoints()).isEqualTo(90);
    }

    @Test
    void customDateRange_isPassedToRepositoryAndSpansYearBoundary() {
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(1L, startDate, endDate))
                .thenReturn(List.of(
                        purchase(alice, "101.00", 2025, 12, 24),  // 52
                        purchase(alice, "60.00", 2026, 1, 3)));  // 10

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L, startDate, endDate);

        assertThat(rewards.monthlyRewards()).containsExactly(
                new MonthlyReward(2025, Month.NOVEMBER, 0),
                new MonthlyReward(2025, Month.DECEMBER, 52),
                new MonthlyReward(2026, Month.JANUARY, 10));
        assertThat(rewards.totalPoints()).isEqualTo(62);
    }

    @Test
    void allCustomers_eachGetTheirOwnPoints_includingCustomersWithNoPurchases() {
        Customer carol = new Customer(3L, "Carol");
        when(customerRepository.findAll(any(Sort.class))).thenReturn(List.of(alice, bob, carol));
        when(transactionRepository.findByTransactionDateBetween(DEFAULT_PERIOD_START, TODAY))
                .thenReturn(List.of(
                        purchase(alice, "120.00", 2026, 1, 5),   // 90
                        purchase(bob, "130.00", 2026, 2, 5),     // 110
                        purchase(bob, "55.00", 2026, 3, 5)));    // 5

        List<CustomerRewardsResponse> allRewards = rewardService.getRewardsForAllCustomers(null, null);

        assertThat(allRewards)
                .extracting(CustomerRewardsResponse::customerName, CustomerRewardsResponse::totalPoints)
                .containsExactly(
                        tuple("Alice", 90L),
                        tuple("Bob", 115L),
                        tuple("Carol", 0L));
        assertThat(allRewards.get(1).monthlyRewards()).containsExactly(
                new MonthlyReward(2026, Month.JANUARY, 0),
                new MonthlyReward(2026, Month.FEBRUARY, 110),
                new MonthlyReward(2026, Month.MARCH, 5));
    }

    @Test
    void unknownCustomer_throwsCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.getRewardsForCustomer(99L, null, null))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void startDateAfterEndDate_isRejected() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> rewardService.getRewardsForAllCustomers(startDate, endDate))
                .isInstanceOf(InvalidDateRangeException.class);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void onlyOneDateProvided_isRejected() {
        assertThatThrownBy(() -> rewardService.getRewardsForAllCustomers(LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    private static Transaction purchase(Customer customer, String amount, int year, int month, int day) {
        return new Transaction(customer, new BigDecimal(amount), LocalDate.of(year, month, day));
    }
}
