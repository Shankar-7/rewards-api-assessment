package com.retailer.rewards.controller;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void getAllTransactions_returnsEachTransactionWithItsPoints() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(
                new TransactionResponse(1L, 1L, "Alice", new BigDecimal("120.00"), LocalDate.of(2026, 1, 5), 90)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Alice"))
                .andExpect(jsonPath("$[0].amount").value(120.00))
                .andExpect(jsonPath("$[0].transactionDate").value("2026-01-05"))
                .andExpect(jsonPath("$[0].rewardPoints").value(90));
    }
}
