package com.test.alo.split_bill_api.dto;

import com.test.alo.split_bill_api.model.ExpenseCategory;
import com.test.alo.split_bill_api.model.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddExpenseRequest {

    @NotBlank(message = "Expense description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "paidBy participant name is required")
    private String paidBy;

    // For EQUAL split, participants who share the cost
    private List<String> participants;

    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Builder.Default
    private ExpenseCategory category = ExpenseCategory.OTHER;

    // Optional list of custom shares if splitType is EXACT or PERCENTAGE
    private List<ExpenseItemShareRequest> customShares;
}
