package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.SettlementResponse;
import com.test.alo.split_bill_api.dto.SettlementTransactionDto;
import com.test.alo.split_bill_api.model.*;
import com.test.alo.split_bill_api.repository.ExpenseRepository;
import com.test.alo.split_bill_api.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

        @Mock
        private BillGroupService billGroupService;

        @Mock
        private ExpenseRepository expenseRepository;

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private ServiceChargeCalculator serviceChargeCalculator;

        @InjectMocks
        private SettlementService settlementService;

        private BillGroup testGroup;
        private Participant alice;
        private Participant bob;
        private Participant charlie;

        @BeforeEach
        void setUp() {
                testGroup = BillGroup.builder()
                                .id(1L)
                                .name("Bali Trip")
                                .participants(new ArrayList<>())
                                .build();

                alice = Participant.builder().id(1L).name("Alice").billGroup(testGroup).build();
                bob = Participant.builder().id(2L).name("Bob").billGroup(testGroup).build();
                charlie = Participant.builder().id(3L).name("Charlie").billGroup(testGroup).build();

                testGroup.getParticipants().addAll(List.of(alice, bob, charlie));
        }

        @Test
        @DisplayName("Should correctly calculate balances and minimize transactions in settlement")
        void testCalculateSettlement_BasicScenario() {
                // Scenario:
                // Expense 1: Alice paid 300.00 for Alice, Bob, Charlie (100.00 each)
                // Expense 2: Bob paid 60.00 for Bob, Charlie (30.00 each)
                // Net Balances:
                // Alice: +300 - 100 = +200.00
                // Bob: +60 - 100 - 30 = -70.00
                // Charlie: 0 - 100 - 30 = -130.00
                // Expected settlements:
                // Charlie pays Alice 130.00
                // Bob pays Alice 70.00

                Expense expense1 = Expense.builder()
                                .id(101L)
                                .billGroup(testGroup)
                                .description("Villa Accommodation")
                                .amount(new BigDecimal("300.00"))
                                .paidBy(alice)
                                .splitType(SplitType.EQUAL)
                                .category(ExpenseCategory.ACCOMMODATION)
                                .shares(new ArrayList<>())
                                .build();
                expense1.getShares().add(ExpenseShare.builder().participant(alice).shareAmount(new BigDecimal("100.00"))
                                .build());
                expense1.getShares().add(
                                ExpenseShare.builder().participant(bob).shareAmount(new BigDecimal("100.00")).build());
                expense1.getShares().add(ExpenseShare.builder().participant(charlie)
                                .shareAmount(new BigDecimal("100.00")).build());

                Expense expense2 = Expense.builder()
                                .id(102L)
                                .billGroup(testGroup)
                                .description("Team Dinner")
                                .amount(new BigDecimal("60.00"))
                                .paidBy(bob)
                                .splitType(SplitType.EQUAL)
                                .category(ExpenseCategory.FOOD)
                                .shares(new ArrayList<>())
                                .build();
                expense2.getShares().add(
                                ExpenseShare.builder().participant(bob).shareAmount(new BigDecimal("30.00")).build());
                expense2.getShares().add(ExpenseShare.builder().participant(charlie)
                                .shareAmount(new BigDecimal("30.00")).build());

                when(billGroupService.findEntityById(1L)).thenReturn(testGroup);
                when(expenseRepository.findByBillGroupIdWithDetails(1L)).thenReturn(List.of(expense1, expense2));
                when(paymentRepository.findByBillGroupIdWithDetails(1L)).thenReturn(List.of());
                when(serviceChargeCalculator.calculateServiceChargePct(any())).thenReturn(5);
                when(serviceChargeCalculator.calculateServiceChargeAmount(any(), eq(5)))
                                .thenReturn(new BigDecimal("18.00"));

                SettlementResponse response = settlementService.calculateSettlement(1L, null);

                assertNotNull(response);
                assertEquals(new BigDecimal("360.00"), response.getTotalExpenses());
                assertEquals(5, response.getServiceChargePct());
                assertEquals(new BigDecimal("18.00"), response.getServiceChargeAmount());

                Map<String, BigDecimal> balances = response.getNetBalances();
                assertEquals(new BigDecimal("200.00"), balances.get("Alice"));
                assertEquals(new BigDecimal("-70.00"), balances.get("Bob"));
                assertEquals(new BigDecimal("-130.00"), balances.get("Charlie"));

                List<SettlementTransactionDto> settlements = response.getSettlements();
                assertEquals(2, settlements.size());

                // Verify total money transferred settles all debts exactly
                BigDecimal totalSettled = settlements.stream()
                                .map(SettlementTransactionDto::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(new BigDecimal("200.00"), totalSettled);

                // Verify category summaries
                assertEquals(new BigDecimal("300.00"), response.getCategorySummaries().get("ACCOMMODATION"));
                assertEquals(new BigDecimal("60.00"), response.getCategorySummaries().get("FOOD"));
        }

        @Test
        @DisplayName("Should adjust balances and settlements when direct payment is recorded")
        void testCalculateSettlement_WithRecordedPayments() {
                // Alice pays 100 for Bob (Bob owes Alice 100)
                Expense expense = Expense.builder()
                                .id(1L)
                                .billGroup(testGroup)
                                .description("Taxi")
                                .amount(new BigDecimal("100.00"))
                                .paidBy(alice)
                                .splitType(SplitType.EQUAL)
                                .category(ExpenseCategory.TRANSPORTATION)
                                .shares(new ArrayList<>())
                                .build();
                expense.getShares().add(
                                ExpenseShare.builder().participant(bob).shareAmount(new BigDecimal("100.00")).build());

                // Bob pays Alice 60 directly
                Payment payment = Payment.builder()
                                .id(1L)
                                .billGroup(testGroup)
                                .paidBy(bob)
                                .paidTo(alice)
                                .amount(new BigDecimal("60.00"))
                                .notes("Partial settlement")
                                .build();

                when(billGroupService.findEntityById(1L)).thenReturn(testGroup);
                when(expenseRepository.findByBillGroupIdWithDetails(1L)).thenReturn(List.of(expense));
                when(paymentRepository.findByBillGroupIdWithDetails(1L)).thenReturn(List.of(payment));
                when(serviceChargeCalculator.calculateServiceChargePct(any())).thenReturn(0);
                when(serviceChargeCalculator.calculateServiceChargeAmount(any(), eq(0)))
                                .thenReturn(new BigDecimal("0.00"));

                SettlementResponse response = settlementService.calculateSettlement(1L, null);

                // Remaining: Bob owes Alice 40.00
                assertEquals(new BigDecimal("40.00"), response.getNetBalances().get("Alice"));
                assertEquals(new BigDecimal("-40.00"), response.getNetBalances().get("Bob"));

                assertEquals(1, response.getSettlements().size());
                assertEquals("Bob", response.getSettlements().get(0).getFrom());
                assertEquals("Alice", response.getSettlements().get(0).getTo());
                assertEquals(new BigDecimal("40.00"), response.getSettlements().get(0).getAmount());
        }

        @Test
        @DisplayName("Should optimize multiple debts into minimal number of transactions")
        void testComputeOptimizedSettlements_Minimization() {
                // Balances:
                // A: +100
                // B: +50
                // C: -80
                // D: -70
                // Total positive = 150, Total negative = -150
                // Naive could take up to 4 transactions.
                // Greedy reduces this to 3 transactions (N-1 where N active is 4).
                Map<String, BigDecimal> balances = Map.of(
                                "A", new BigDecimal("100.00"),
                                "B", new BigDecimal("50.00"),
                                "C", new BigDecimal("-80.00"),
                                "D", new BigDecimal("-70.00"));

                List<SettlementTransactionDto> settlements = settlementService.computeOptimizedSettlements(balances);

                // Transaction count should be <= 3
                assertEquals(3, settlements.size());

                BigDecimal totalSettled = settlements.stream()
                                .map(SettlementTransactionDto::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(new BigDecimal("150.00"), totalSettled);
        }
}
