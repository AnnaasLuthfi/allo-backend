package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.PaymentResponse;
import com.test.alo.split_bill_api.dto.RecordPaymentRequest;
import com.test.alo.split_bill_api.exception.BadRequestException;
import com.test.alo.split_bill_api.model.BillGroup;
import com.test.alo.split_bill_api.model.Participant;
import com.test.alo.split_bill_api.model.Payment;
import com.test.alo.split_bill_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BillGroupService billGroupService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse recordPayment(Long groupId, RecordPaymentRequest request) {
        BillGroup group = billGroupService.findEntityById(groupId);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        if (request.getPaidBy().trim().equalsIgnoreCase(request.getPaidTo().trim())) {
            throw new BadRequestException("Payer and receiver cannot be the same participant");
        }

        Participant payer = group.getParticipants().stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getPaidBy().trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Payer '" + request.getPaidBy() + "' not found in group"));

        Participant receiver = group.getParticipants().stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getPaidTo().trim()))
                .findFirst()
                .orElseThrow(
                        () -> new BadRequestException("Receiver '" + request.getPaidTo() + "' not found in group"));

        Payment payment = Payment.builder()
                .billGroup(group)
                .paidBy(payer)
                .paidTo(receiver)
                .amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getGroupPayments(Long groupId) {
        billGroupService.findEntityById(groupId);
        return paymentRepository.findByBillGroupIdWithDetails(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .groupId(payment.getBillGroup().getId())
                .paidBy(payment.getPaidBy().getName())
                .paidTo(payment.getPaidTo().getName())
                .amount(payment.getAmount())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
