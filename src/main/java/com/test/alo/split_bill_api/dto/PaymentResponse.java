package com.test.alo.split_bill_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long groupId;
    private String paidBy;
    private String paidTo;
    private BigDecimal amount;
    private String notes;
    private LocalDateTime createdAt;
}
