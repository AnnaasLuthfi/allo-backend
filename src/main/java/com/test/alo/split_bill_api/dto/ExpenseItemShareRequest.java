package com.test.alo.split_bill_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseItemShareRequest {
    private String participantName;
    private BigDecimal amount;
    private BigDecimal percentage;
}
