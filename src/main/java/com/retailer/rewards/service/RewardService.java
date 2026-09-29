package com.retailer.rewards.service;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyReward;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.RewardPeriod;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;

@Service
@Transactional(readOnly = true)
public class RewardService {

    /** The current month plus the two before it. */
    private static final int DEFAULT_PERIOD_MONTHS = 3;

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RewardPointsCalculator pointsCalculator;
    private final Clock clock;

    public RewardService(CustomerRepository customerRepository,
                         TransactionRepository transactionRepository,
                         RewardPointsCalculator pointsCalculator,
                         Clock clock) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.pointsCalculator = pointsCalculator;
        this.clock = clock;
    }

    public List<CustomerRewardsResponse> getRewardsForAllCustomers(LocalDate startDate, LocalDate endDate) {
        RewardPeriod period = resolvePeriod(startDate, endDate);

        Map<Long, List<Transaction>> transactionsByCustomerId = transactionRepository
                .findByTransactionDateBetween(period.startDate(), period.endDate())
                .stream()
                .collect(groupingBy(transaction -> transaction.getCustomer().getId()));

        return customerRepository.findAll(Sort.by("id")).stream()
                .map(customer -> buildCustomerRewards(
                        customer,
                        transactionsByCustomerId.getOrDefault(customer.getId(), List.of()),
                        period))
                .toList();
    }

    public CustomerRewardsResponse getRewardsForCustomer(Long customerId, LocalDate startDate, LocalDate endDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        RewardPeriod period = resolvePeriod(startDate, endDate);

        List<Transaction> customerTransactions = transactionRepository
                .findByCustomerIdAndTransactionDateBetween(customerId, period.startDate(), period.endDate());

        return buildCustomerRewards(customer, customerTransactions, period);
    }

    /**
     * Uses the caller's dates when both are given; otherwise defaults to the
     * first day of the month two months ago through today.
     */
    private RewardPeriod resolvePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            LocalDate today = LocalDate.now(clock);
            LocalDate firstDayOfPeriod = YearMonth.from(today)
                    .minusMonths(DEFAULT_PERIOD_MONTHS - 1)
                    .atDay(1);
            return new RewardPeriod(firstDayOfPeriod, today);
        }
        if (startDate == null || endDate == null) {
            throw new InvalidDateRangeException("startDate and endDate must be provided together");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("startDate must not be after endDate");
        }
        return new RewardPeriod(startDate, endDate);
    }

    private CustomerRewardsResponse buildCustomerRewards(Customer customer,
                                                         List<Transaction> transactions,
                                                         RewardPeriod period) {
        Map<YearMonth, Long> pointsByMonth = transactions.stream()
                .collect(groupingBy(
                        transaction -> YearMonth.from(transaction.getTransactionDate()),
                        summingLong(transaction -> pointsCalculator.calculatePoints(transaction.getAmount()))));

        List<MonthlyReward> monthlyRewards = period.months().stream()
                .map(month -> new MonthlyReward(
                        month.getYear(),
                        month.getMonth(),
                        pointsByMonth.getOrDefault(month, 0L)))
                .toList();

        long totalPoints = monthlyRewards.stream()
                .mapToLong(MonthlyReward::points)
                .sum();

        return new CustomerRewardsResponse(
                customer.getId(),
                customer.getName(),
                period.startDate(),
                period.endDate(),
                monthlyRewards,
                totalPoints);
    }
}
