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
public class SettlementTransactionDto {
    private String from;
    private String to;
    private BigDecimal amount;
}
