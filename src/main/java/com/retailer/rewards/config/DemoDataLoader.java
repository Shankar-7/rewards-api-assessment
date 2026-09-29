package com.retailer.rewards.config;

import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Seeds the in-memory database with demo data at startup.
 * Dates are relative to today so the default three-month window always has data.
 * The amounts deliberately include the $50 / $100 boundaries, cents, a customer with
 * no purchases, and one purchase that falls outside the three-month window.
 */
@Component
public class DemoDataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public DemoDataLoader(CustomerRepository customerRepository,
                          TransactionRepository transactionRepository,
                          Clock clock) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate lastMonth = currentMonth.minusMonths(1).atDay(10);
        LocalDate twoMonthsAgo = currentMonth.minusMonths(2).atDay(5);
        LocalDate threeMonthsAgo = currentMonth.minusMonths(3).atDay(20);

        Customer alice = customerRepository.save(new Customer("Alice Johnson"));
        Customer bob = customerRepository.save(new Customer("Bob Smith"));
        Customer carol = customerRepository.save(new Customer("Carol Davis"));
        customerRepository.save(new Customer("Dan Brown")); // no purchases

        transactionRepository.saveAll(List.of(
                new Transaction(alice, new BigDecimal("300.00"), threeMonthsAgo),   // outside window
                new Transaction(alice, new BigDecimal("120.00"), twoMonthsAgo),     // 90
                new Transaction(alice, new BigDecimal("75.00"), twoMonthsAgo.plusDays(7)), // 25
                new Transaction(alice, new BigDecimal("100.00"), lastMonth),        // 50
                new Transaction(alice, new BigDecimal("50.00"), lastMonth.plusDays(3)),    // 0
                new Transaction(alice, new BigDecimal("200.00"), today),            // 250

                new Transaction(bob, new BigDecimal("49.99"), twoMonthsAgo),        // 0
                new Transaction(bob, new BigDecimal("100.01"), lastMonth),          // 50
                new Transaction(bob, new BigDecimal("130.75"), today),              // 110

                new Transaction(carol, new BigDecimal("250.00"), twoMonthsAgo.plusDays(2)), // 350
                new Transaction(carol, new BigDecimal("55.50"), today)              // 5
        ));
    }
}
