package com.test.alo.split_bill_api.controller;

import com.test.alo.split_bill_api.dto.PaymentResponse;
import com.test.alo.split_bill_api.dto.RecordPaymentRequest;
import com.test.alo.split_bill_api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long groupId,
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getGroupPayments(@PathVariable Long groupId) {
        return ResponseEntity.ok(paymentService.getGroupPayments(groupId));
    }
}
