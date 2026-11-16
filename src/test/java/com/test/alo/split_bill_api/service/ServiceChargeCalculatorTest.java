package com.test.alo.split_bill_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceChargeCalculatorTest {

    private ServiceChargeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ServiceChargeCalculator();
    }

    @Test
    @DisplayName("Should match the official example 'johndoe47' yielding 0%")
    void testCalculateServiceChargePct_JohnDoe47() {
        // johndoe47:
        // j=106, o=111, h=104, n=110, d=100, o=111, e=101, 4=52, 7=55 => 850 % 10 = 0
        int pct = calculator.calculateServiceChargePct("johndoe47");
        assertEquals(0, pct);
    }

    @Test
    @DisplayName("Should handle case insensitivity correctly")
    void testCalculateServiceChargePct_CaseInsensitive() {
        // JohnDoe47 should be lowercase to johndoe47 -> same result
        int pctUpper = calculator.calculateServiceChargePct("JohnDoe47");
        int pctLower = calculator.calculateServiceChargePct("johndoe47");
        assertEquals(pctLower, pctUpper);
    }

    @Test
    @DisplayName("Should correctly calculate service_charge_amount from total expenses")
    void testCalculateServiceChargeAmount() {
        BigDecimal totalExpenses = new BigDecimal("350.00");
        int pct = 5;

        // 350.00 * 5% = 17.50
        BigDecimal serviceCharge = calculator.calculateServiceChargeAmount(totalExpenses, pct);
        assertEquals(new BigDecimal("17.50"), serviceCharge);
    }

    @Test
    @DisplayName("Should return 0.00 service charge when pct is 0 or expenses zero")
    void testCalculateServiceChargeAmount_Zero() {
        BigDecimal totalExpenses = new BigDecimal("1000.00");
        BigDecimal charge = calculator.calculateServiceChargeAmount(totalExpenses, 0);
        assertEquals(new BigDecimal("0.00"), charge);

        BigDecimal chargeZeroExpense = calculator.calculateServiceChargeAmount(BigDecimal.ZERO, 5);
        assertEquals(new BigDecimal("0.00"), chargeZeroExpense);
    }
}
