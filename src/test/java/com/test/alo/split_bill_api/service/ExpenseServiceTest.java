package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.AddExpenseRequest;
import com.test.alo.split_bill_api.dto.ExpenseItemShareRequest;
import com.test.alo.split_bill_api.dto.ExpenseResponse;
import com.test.alo.split_bill_api.dto.ExpenseShareResponse;
import com.test.alo.split_bill_api.exception.BadRequestException;
import com.test.alo.split_bill_api.model.*;
import com.test.alo.split_bill_api.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

        @Mock
        private BillGroupService billGroupService;

        @Mock
        private ExpenseRepository expenseRepository;

        @InjectMocks
        private ExpenseService expenseService;

        private BillGroup testGroup;
        private Participant alice;
        private Participant bob;
        private Participant charlie;

        @BeforeEach
        void setUp() {
                testGroup = BillGroup.builder()
                                .id(1L)
                                .name("Office Lunch")
                                .participants(new ArrayList<>())
                                .build();

                alice = Participant.builder().id(1L).name("Alice").billGroup(testGroup).build();
                bob = Participant.builder().id(2L).name("Bob").billGroup(testGroup).build();
                charlie = Participant.builder().id(3L).name("Charlie").billGroup(testGroup).build();

                testGroup.getParticipants().addAll(List.of(alice, bob, charlie));
        }

        @Test
        @DisplayName("Should distribute remainder cents properly on equal split to avoid losing money")
        void testEqualSplitWithRemainderCents() {
                when(billGroupService.findEntityById(1L)).thenReturn(testGroup);
                when(expenseRepository.save(any(Expense.class))).then(returnsFirstArg());

                // Amount = 100.00 across 3 people
                // 100 / 3 = 33.33 with 0.01 remainder
                // First participant gets 33.34, other two get 33.33
                AddExpenseRequest request = AddExpenseRequest.builder()
                                .description("Pizza")
                                .amount(new BigDecimal("100.00"))
                                .paidBy("Alice")
                                .splitType(SplitType.EQUAL)
                                .participants(List.of("Alice", "Bob", "Charlie"))
                                .category(ExpenseCategory.FOOD)
                                .build();

                ExpenseResponse response = expenseService.addExpense(1L, request);

                assertNotNull(response);
                assertEquals(new BigDecimal("100.00"), response.getAmount());
                assertEquals(3, response.getShares().size());

                BigDecimal sumShares = response.getShares().stream()
                                .map(ExpenseShareResponse::getShareAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Sum of shares MUST exactly equal 100.00 without any penny lost
                assertEquals(new BigDecimal("100.00"), sumShares);

                // Verify individual shares
                assertEquals(new BigDecimal("33.34"), response.getShares().get(0).getShareAmount());
                assertEquals(new BigDecimal("33.33"), response.getShares().get(1).getShareAmount());
                assertEquals(new BigDecimal("33.33"), response.getShares().get(2).getShareAmount());
        }

        @Test
        @DisplayName("Should support EXACT split and validate that share sum equals total amount")
        void testExactSplit() {
                when(billGroupService.findEntityById(1L)).thenReturn(testGroup);
                when(expenseRepository.save(any(Expense.class))).then(returnsFirstArg());

                AddExpenseRequest request = AddExpenseRequest.builder()
                                .description("Custom Meal")
                                .amount(new BigDecimal("50.00"))
                                .paidBy("Bob")
                                .splitType(SplitType.EXACT)
                                .customShares(List.of(
                                                ExpenseItemShareRequest.builder().participantName("Alice")
                                                                .amount(new BigDecimal("20.00")).build(),
                                                ExpenseItemShareRequest.builder().participantName("Bob")
                                                                .amount(new BigDecimal("15.00")).build(),
                                                ExpenseItemShareRequest.builder().participantName("Charlie")
                                                                .amount(new BigDecimal("15.00")).build()))
                                .build();

                ExpenseResponse response = expenseService.addExpense(1L, request);
                assertNotNull(response);
                assertEquals(new BigDecimal("50.00"), response.getAmount());
        }

        @Test
        @DisplayName("Should throw BadRequestException when EXACT shares don't sum to total amount")
        void testExactSplit_MismatchedSum() {
                when(billGroupService.findEntityById(1L)).thenReturn(testGroup);

                AddExpenseRequest request = AddExpenseRequest.builder()
                                .description("Custom Meal")
                                .amount(new BigDecimal("50.00"))
                                .paidBy("Bob")
                                .splitType(SplitType.EXACT)
                                .customShares(List.of(
                                                ExpenseItemShareRequest.builder().participantName("Alice")
                                                                .amount(new BigDecimal("20.00")).build(),
                                                ExpenseItemShareRequest.builder().participantName("Bob")
                                                                .amount(new BigDecimal("10.00")).build()))
                                .build();

                assertThrows(BadRequestException.class, () -> expenseService.addExpense(1L, request));
        }
}
