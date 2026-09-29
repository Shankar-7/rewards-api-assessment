package com.retailer.rewards.service;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RewardPointsCalculator pointsCalculator;

    public TransactionService(TransactionRepository transactionRepository, RewardPointsCalculator pointsCalculator) {
        this.transactionRepository = transactionRepository;
        this.pointsCalculator = pointsCalculator;
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomer().getId(),
                transaction.getCustomer().getName(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                pointsCalculator.calculatePoints(transaction.getAmount()));
    }
}
