package com.test.alo.split_bill_api.controller;

import com.test.alo.split_bill_api.dto.CreateGroupRequest;
import com.test.alo.split_bill_api.dto.GroupResponse;
import com.test.alo.split_bill_api.service.BillGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class BillGroupController {

    private final BillGroupService billGroupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupResponse response = billGroupService.createGroup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long id) {
        return ResponseEntity.ok(billGroupService.getGroup(id));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        return ResponseEntity.ok(billGroupService.getAllGroups());
    }
}
