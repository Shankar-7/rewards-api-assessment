package com.retailer.rewards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the full application against the seeded demo data (see DemoDataLoader).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allCustomersAreReturnedWithCorrectTotals() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].totalPoints").value(415))
                .andExpect(jsonPath("$[1].totalPoints").value(160))
                .andExpect(jsonPath("$[2].totalPoints").value(355))
                .andExpect(jsonPath("$[3].totalPoints").value(0));
    }

    @Test
    void singleCustomerHasThreeMonthsOfPoints_andExcludesOlderPurchases() throws Exception {
        mockMvc.perform(get("/api/rewards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRewards.length()").value(3))
                .andExpect(jsonPath("$.monthlyRewards[0].points").value(115))
                .andExpect(jsonPath("$.monthlyRewards[1].points").value(50))
                .andExpect(jsonPath("$.monthlyRewards[2].points").value(250))
                .andExpect(jsonPath("$.totalPoints").value(415));
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/rewards/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transactionsEndpointReturnsAllSeededTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
    }
}
