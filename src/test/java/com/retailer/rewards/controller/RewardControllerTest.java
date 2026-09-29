package com.retailer.rewards.controller;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyReward;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardController.class)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardService rewardService;

    private final CustomerRewardsResponse aliceRewards = new CustomerRewardsResponse(
            1L, "Alice",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 15),
            List.of(new MonthlyReward(2026, Month.JANUARY, 115),
                    new MonthlyReward(2026, Month.FEBRUARY, 50),
                    new MonthlyReward(2026, Month.MARCH, 250)),
            415);

    @Test
    void getRewardsForAllCustomers_returnsListOfCustomerRewards() throws Exception {
        when(rewardService.getRewardsForAllCustomers(null, null)).thenReturn(List.of(aliceRewards));

        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Alice"))
                .andExpect(jsonPath("$[0].totalPoints").value(415));
    }

    @Test
    void getRewardsForCustomer_returnsMonthlyBreakdownAndTotal() throws Exception {
        when(rewardService.getRewardsForCustomer(1L, null, null)).thenReturn(aliceRewards);

        mockMvc.perform(get("/api/rewards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.periodStart").value("2026-01-01"))
                .andExpect(jsonPath("$.monthlyRewards.length()").value(3))
                .andExpect(jsonPath("$.monthlyRewards[0].year").value(2026))
                .andExpect(jsonPath("$.monthlyRewards[0].month").value("JANUARY"))
                .andExpect(jsonPath("$.monthlyRewards[0].points").value(115))
                .andExpect(jsonPath("$.totalPoints").value(415));
    }

    @Test
    void getRewardsForCustomer_passesDateRangeToService() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 15);
        when(rewardService.getRewardsForCustomer(1L, startDate, endDate)).thenReturn(aliceRewards);

        mockMvc.perform(get("/api/rewards/1")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(415));
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        when(rewardService.getRewardsForCustomer(99L, null, null)).thenThrow(new CustomerNotFoundException(99L));

        mockMvc.perform(get("/api/rewards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found with id 99"));
    }

    @Test
    void invalidDateRange_returns400() throws Exception {
        when(rewardService.getRewardsForAllCustomers(any(), any()))
                .thenThrow(new InvalidDateRangeException("startDate must not be after endDate"));

        mockMvc.perform(get("/api/rewards")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("startDate must not be after endDate"));
    }

    @Test
    void badlyFormattedDate_returns400() throws Exception {
        mockMvc.perform(get("/api/rewards").param("startDate", "not-a-date").param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericCustomerId_returns400() throws Exception {
        mockMvc.perform(get("/api/rewards/abc"))
                .andExpect(status().isBadRequest());
    }
}
