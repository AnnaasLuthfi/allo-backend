package com.test.alo.split_bill_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.alo.split_bill_api.dto.AddExpenseRequest;
import com.test.alo.split_bill_api.dto.CreateGroupRequest;
import com.test.alo.split_bill_api.dto.RecordPaymentRequest;
import com.test.alo.split_bill_api.model.ExpenseCategory;
import com.test.alo.split_bill_api.model.SplitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SplitBillIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete E2E workflow: Create group -> Add expense -> Record payment -> Get settlement")
    void testCompleteE2EWorkflow() throws Exception {
        // 1. Create a bill group
        CreateGroupRequest groupReq = CreateGroupRequest.builder()
                .name("Bandung Weekend")
                .participants(List.of("Rian", "Siti", "Budi"))
                .build();

        String groupResponseStr = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Bandung Weekend"))
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        Number groupIdNum = com.jayway.jsonpath.JsonPath.read(groupResponseStr, "$.id");
        Long groupId = groupIdNum.longValue();

        // 2. Add an expense: Rian pays 300,000 for all 3 members (100,000 each)
        AddExpenseRequest expenseReq1 = AddExpenseRequest.builder()
                .description("Hotel Booking")
                .amount(new BigDecimal("300000.00"))
                .paidBy("Rian")
                .splitType(SplitType.EQUAL)
                .category(ExpenseCategory.ACCOMMODATION)
                .build();

        mockMvc.perform(post("/api/groups/" + groupId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(300000.00))
                .andExpect(jsonPath("$.shares", hasSize(3)));

        // 3. Add an expense: Siti pays 60,000 for Siti and Budi (30,000 each)
        AddExpenseRequest expenseReq2 = AddExpenseRequest.builder()
                .description("Dinner")
                .amount(new BigDecimal("60000.00"))
                .paidBy("Siti")
                .splitType(SplitType.EQUAL)
                .category(ExpenseCategory.FOOD)
                .participants(List.of("Siti", "Budi"))
                .build();

        mockMvc.perform(post("/api/groups/" + groupId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq2)))
                .andExpect(status().isCreated());

        // 4. Record a direct payment: Budi pays Rian 50,000
        RecordPaymentRequest paymentReq = RecordPaymentRequest.builder()
                .paidBy("Budi")
                .paidTo("Rian")
                .amount(new BigDecimal("50000.00"))
                .notes("Transfer via Allo Bank")
                .build();

        mockMvc.perform(post("/api/groups/" + groupId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50000.00));

        // 5. Query Settlement with specific githubUsername override
        mockMvc.perform(get("/api/groups/" + groupId + "/settlement")
                        .param("githubUsername", "johndoe47"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId))
                .andExpect(jsonPath("$.totalExpenses").value(360000.00))
                .andExpect(jsonPath("$.service_charge_pct").value(0))
                .andExpect(jsonPath("$.service_charge_amount").value(0.00))
                .andExpect(jsonPath("$.settlements").isArray())
                .andExpect(jsonPath("$.categorySummaries.ACCOMMODATION").value(300000.00))
                .andExpect(jsonPath("$.categorySummaries.FOOD").value(60000.00));
    }
}
