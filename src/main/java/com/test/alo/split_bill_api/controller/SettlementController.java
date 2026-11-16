package com.test.alo.split_bill_api.controller;

import com.test.alo.split_bill_api.dto.SettlementResponse;
import com.test.alo.split_bill_api.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable Long groupId,
            @RequestParam(required = false) String githubUsername) {
        SettlementResponse response = settlementService.calculateSettlement(groupId, githubUsername);
        return ResponseEntity.ok(response);
    }
}
