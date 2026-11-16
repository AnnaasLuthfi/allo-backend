package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.AddExpenseRequest;
import com.test.alo.split_bill_api.dto.ExpenseItemShareRequest;
import com.test.alo.split_bill_api.dto.ExpenseResponse;
import com.test.alo.split_bill_api.dto.ExpenseShareResponse;
import com.test.alo.split_bill_api.exception.BadRequestException;
import com.test.alo.split_bill_api.model.*;
import com.test.alo.split_bill_api.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final BillGroupService billGroupService;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public ExpenseResponse addExpense(Long groupId, AddExpenseRequest request) {
        BillGroup group = billGroupService.findEntityById(groupId);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Expense amount must be strictly greater than zero");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        Participant payer = group.getParticipants().stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getPaidBy().trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Payer '" + request.getPaidBy() + "' is not a participant of group: " + group.getName()));

        SplitType splitType = request.getSplitType() != null ? request.getSplitType() : SplitType.EQUAL;
        ExpenseCategory category = request.getCategory() != null ? request.getCategory() : ExpenseCategory.OTHER;

        Expense expense = Expense.builder()
                .billGroup(group)
                .description(request.getDescription().trim())
                .amount(amount)
                .paidBy(payer)
                .splitType(splitType)
                .category(category)
                .build();

        Map<Participant, BigDecimal> calculatedShares = calculateShares(group, amount, splitType, request);

        for (Map.Entry<Participant, BigDecimal> entry : calculatedShares.entrySet()) {
            ExpenseShare share = ExpenseShare.builder()
                    .expense(expense)
                    .participant(entry.getKey())
                    .shareAmount(entry.getValue())
                    .build();
            expense.getShares().add(share);
        }

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId) {
        billGroupService.findEntityById(groupId); // validate existence
        return expenseRepository.findByBillGroupIdWithDetails(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Map<Participant, BigDecimal> calculateShares(
            BillGroup group,
            BigDecimal totalAmount,
            SplitType splitType,
            AddExpenseRequest request) {

        Map<String, Participant> participantMap = group.getParticipants().stream()
                .collect(Collectors.toMap(p -> p.getName().toLowerCase(), p -> p));

        Map<Participant, BigDecimal> result = new LinkedHashMap<>();

        switch (splitType) {
            case EQUAL -> {
                List<Participant> beneficiaries = new ArrayList<>();
                if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
                    beneficiaries.addAll(group.getParticipants());
                } else {
                    for (String name : request.getParticipants()) {
                        Participant p = participantMap.get(name.trim().toLowerCase());
                        if (p == null) {
                            throw new BadRequestException(
                                    "Beneficiary '" + name + "' is not in group: " + group.getName());
                        }
                        if (!beneficiaries.contains(p)) {
                            beneficiaries.add(p);
                        }
                    }
                }

                if (beneficiaries.isEmpty()) {
                    throw new BadRequestException("Expense must have at least one beneficiary");
                }

                int count = beneficiaries.size();
                BigDecimal countBd = BigDecimal.valueOf(count);

                BigDecimal baseShare = totalAmount.divide(countBd, 2, RoundingMode.FLOOR);
                BigDecimal distributedSum = baseShare.multiply(countBd);
                BigDecimal remainder = totalAmount.subtract(distributedSum);
                int remainderCents = remainder.movePointRight(2).intValueExact();

                for (int i = 0; i < count; i++) {
                    Participant p = beneficiaries.get(i);
                    BigDecimal share = baseShare;
                    if (i < remainderCents) {
                        share = share.add(new BigDecimal("0.01"));
                    }
                    result.put(p, share);
                }
            }

            case EXACT -> {
                if (request.getCustomShares() == null || request.getCustomShares().isEmpty()) {
                    throw new BadRequestException("customShares list is required for EXACT split type");
                }

                BigDecimal runningSum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                for (ExpenseItemShareRequest item : request.getCustomShares()) {
                    if (item.getParticipantName() == null || item.getAmount() == null) {
                        throw new BadRequestException("customShare must have participantName and amount");
                    }
                    Participant p = participantMap.get(item.getParticipantName().trim().toLowerCase());
                    if (p == null) {
                        throw new BadRequestException("Participant '" + item.getParticipantName() + "' not in group");
                    }
                    if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new BadRequestException("Share amount cannot be negative");
                    }
                    BigDecimal share = item.getAmount().setScale(2, RoundingMode.HALF_UP);
                    result.put(p, share);
                    runningSum = runningSum.add(share);
                }

                if (runningSum.compareTo(totalAmount) != 0) {
                    throw new BadRequestException("Sum of exact shares (" + runningSum +
                            ") does not equal total expense amount (" + totalAmount + ")");
                }
            }

            case PERCENTAGE -> {
                if (request.getCustomShares() == null || request.getCustomShares().isEmpty()) {
                    throw new BadRequestException("customShares list is required for PERCENTAGE split type");
                }

                BigDecimal totalPct = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                List<Map.Entry<Participant, BigDecimal>> pctEntries = new ArrayList<>();

                for (ExpenseItemShareRequest item : request.getCustomShares()) {
                    if (item.getParticipantName() == null || item.getPercentage() == null) {
                        throw new BadRequestException("customShare must have participantName and percentage");
                    }
                    Participant p = participantMap.get(item.getParticipantName().trim().toLowerCase());
                    if (p == null) {
                        throw new BadRequestException("Participant '" + item.getParticipantName() + "' not in group");
                    }
                    BigDecimal pct = item.getPercentage().setScale(2, RoundingMode.HALF_UP);
                    if (pct.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BadRequestException("Percentage cannot be negative");
                    }
                    totalPct = totalPct.add(pct);
                    pctEntries.add(Map.entry(p, pct));
                }

                if (totalPct.compareTo(new BigDecimal("100.00")) != 0) {
                    throw new BadRequestException("Total percentage shares must equal 100%, got: " + totalPct + "%");
                }

                BigDecimal allocatedSum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                for (int i = 0; i < pctEntries.size(); i++) {
                    Participant p = pctEntries.get(i).getKey();
                    BigDecimal pct = pctEntries.get(i).getValue();
                    BigDecimal share = totalAmount.multiply(pct)
                            .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

                    result.put(p, share);
                    allocatedSum = allocatedSum.add(share);
                }

                BigDecimal diff = totalAmount.subtract(allocatedSum);
                if (diff.compareTo(BigDecimal.ZERO) != 0 && !pctEntries.isEmpty()) {
                    Participant firstParticipant = pctEntries.get(0).getKey();
                    result.put(firstParticipant, result.get(firstParticipant).add(diff));
                }
            }
        }

        return result;
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        List<ExpenseShareResponse> shareResponses = expense.getShares().stream()
                .map(s -> ExpenseShareResponse.builder()
                        .participantName(s.getParticipant().getName())
                        .shareAmount(s.getShareAmount())
                        .build())
                .collect(Collectors.toList());

        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getBillGroup().getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .paidBy(expense.getPaidBy().getName())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .shares(shareResponses)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
