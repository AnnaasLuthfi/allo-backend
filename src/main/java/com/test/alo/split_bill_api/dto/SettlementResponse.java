package com.test.alo.split_bill_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private Long groupId;
    private String groupName;
    private BigDecimal totalExpenses;

    @JsonProperty("service_charge_pct")
    private Integer serviceChargePct;

    @JsonProperty("service_charge_amount")
    private BigDecimal serviceChargeAmount;

    private Map<String, BigDecimal> netBalances;
    private List<SettlementTransactionDto> settlements;
    private Map<String, BigDecimal> categorySummaries;
}
