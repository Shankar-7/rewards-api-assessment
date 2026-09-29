package com.retailer.rewards.repository;

import com.retailer.rewards.model.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = "customer")
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByCustomerIdAndTransactionDateBetween(Long customerId, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = "customer")
    List<Transaction> findAllByOrderByTransactionDateAscIdAsc();
}
