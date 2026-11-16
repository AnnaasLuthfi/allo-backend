package com.test.alo.split_bill_api.dto;

import com.test.alo.split_bill_api.model.ExpenseCategory;
import com.test.alo.split_bill_api.model.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long groupId;
    private String description;
    private BigDecimal amount;
    private String paidBy;
    private SplitType splitType;
    private ExpenseCategory category;
    private List<ExpenseShareResponse> shares;
    private LocalDateTime createdAt;
}
