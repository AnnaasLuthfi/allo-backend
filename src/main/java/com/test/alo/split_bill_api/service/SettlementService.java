package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.SettlementResponse;
import com.test.alo.split_bill_api.dto.SettlementTransactionDto;
import com.test.alo.split_bill_api.model.BillGroup;
import com.test.alo.split_bill_api.model.Expense;
import com.test.alo.split_bill_api.model.ExpenseShare;
import com.test.alo.split_bill_api.model.Payment;
import com.test.alo.split_bill_api.repository.ExpenseRepository;
import com.test.alo.split_bill_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final BillGroupService billGroupService;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceChargeCalculator serviceChargeCalculator;

    @Transactional(readOnly = true)
    public SettlementResponse calculateSettlement(Long groupId, String githubUsernameOverride) {
        BillGroup group = billGroupService.findEntityById(groupId);
        List<Expense> expenses = expenseRepository.findByBillGroupIdWithDetails(groupId);
        List<Payment> payments = paymentRepository.findByBillGroupIdWithDetails(groupId);

        BigDecimal totalExpenses = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> categorySummaries = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            BigDecimal amt = expense.getAmount();
            totalExpenses = totalExpenses.add(amt);

            String catName = expense.getCategory().name();
            categorySummaries.put(catName,
                    categorySummaries.getOrDefault(catName, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .add(amt));
        }

        Map<String, BigDecimal> netBalances = new LinkedHashMap<>();
        for (var p : group.getParticipants()) {
            netBalances.put(p.getName(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        for (Expense expense : expenses) {
            String payerName = expense.getPaidBy().getName();
            netBalances.put(payerName, netBalances.get(payerName).add(expense.getAmount()));

            for (ExpenseShare share : expense.getShares()) {
                String benName = share.getParticipant().getName();
                netBalances.put(benName, netBalances.get(benName).subtract(share.getShareAmount()));
            }
        }

        for (Payment payment : payments) {
            String payer = payment.getPaidBy().getName();
            String receiver = payment.getPaidTo().getName();

            netBalances.put(payer, netBalances.get(payer).add(payment.getAmount()));
            netBalances.put(receiver, netBalances.get(receiver).subtract(payment.getAmount()));
        }

        List<SettlementTransactionDto> settlements = computeOptimizedSettlements(netBalances);

        int serviceChargePct = serviceChargeCalculator.calculateServiceChargePct(githubUsernameOverride);
        BigDecimal serviceChargeAmount = serviceChargeCalculator.calculateServiceChargeAmount(totalExpenses,
                serviceChargePct);

        return SettlementResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .totalExpenses(totalExpenses)
                .serviceChargePct(serviceChargePct)
                .serviceChargeAmount(serviceChargeAmount)
                .netBalances(netBalances)
                .settlements(settlements)
                .categorySummaries(categorySummaries)
                .build();
    }

    public List<SettlementTransactionDto> computeOptimizedSettlements(Map<String, BigDecimal> balances) {
        List<SettlementTransactionDto> transactions = new ArrayList<>();

        PriorityQueue<BalanceNode> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<BalanceNode> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        BigDecimal zeroThreshold = new BigDecimal("0.005");

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            BigDecimal bal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(zeroThreshold) > 0) {
                creditors.offer(new BalanceNode(entry.getKey(), bal));
            } else if (bal.compareTo(zeroThreshold.negate()) < 0) {
                debtors.offer(new BalanceNode(entry.getKey(), bal.abs()));
            }
        }

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            BalanceNode debtor = debtors.poll();
            BalanceNode creditor = creditors.poll();

            BigDecimal transfer = debtor.amount.min(creditor.amount).setScale(2, RoundingMode.HALF_UP);

            transactions.add(SettlementTransactionDto.builder()
                    .from(debtor.name)
                    .to(creditor.name)
                    .amount(transfer)
                    .build());

            BigDecimal debtorRemaining = debtor.amount.subtract(transfer).setScale(2, RoundingMode.HALF_UP);
            BigDecimal creditorRemaining = creditor.amount.subtract(transfer).setScale(2, RoundingMode.HALF_UP);

            if (debtorRemaining.compareTo(zeroThreshold) > 0) {
                debtors.offer(new BalanceNode(debtor.name, debtorRemaining));
            }
            if (creditorRemaining.compareTo(zeroThreshold) > 0) {
                creditors.offer(new BalanceNode(creditor.name, creditorRemaining));
            }
        }

        return transactions;
    }

    private static class BalanceNode {
        String name;
        BigDecimal amount;

        BalanceNode(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
