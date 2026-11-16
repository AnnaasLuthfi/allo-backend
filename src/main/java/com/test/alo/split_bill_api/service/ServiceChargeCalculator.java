package com.test.alo.split_bill_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ServiceChargeCalculator {

    @Value("${app.github.username:allobankcandidate}")
    private String configuredGithubUsername;

    public int calculateServiceChargePct(String githubUsername) {
        String username = (githubUsername != null && !githubUsername.isBlank())
                ? githubUsername
                : configuredGithubUsername;

        if (username == null || username.isBlank()) {
            return 0;
        }

        String normalized = username.trim().toLowerCase();
        int sum = 0;
        for (char c : normalized.toCharArray()) {
            sum += c;
        }
        return sum % 10;
    }

    public BigDecimal calculateServiceChargeAmount(BigDecimal totalExpenses, int serviceChargePct) {
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalExpenses.multiply(BigDecimal.valueOf(serviceChargePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public String getConfiguredGithubUsername() {
        return configuredGithubUsername;
    }
}
