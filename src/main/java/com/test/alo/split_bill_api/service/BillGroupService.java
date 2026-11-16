package com.test.alo.split_bill_api.service;

import com.test.alo.split_bill_api.dto.CreateGroupRequest;
import com.test.alo.split_bill_api.dto.GroupResponse;
import com.test.alo.split_bill_api.exception.BadRequestException;
import com.test.alo.split_bill_api.exception.ResourceNotFoundException;
import com.test.alo.split_bill_api.model.BillGroup;
import com.test.alo.split_bill_api.model.Participant;
import com.test.alo.split_bill_api.repository.BillGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillGroupService {

    private final BillGroupRepository billGroupRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Group name cannot be empty");
        }

        if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
            throw new BadRequestException("Group must have at least one participant");
        }

        List<String> cleanedNames = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (String p : request.getParticipants()) {
            if (p == null || p.trim().isEmpty()) {
                throw new BadRequestException("Participant name cannot be blank");
            }
            String trimmed = p.trim();
            if (!seenNames.add(trimmed.toLowerCase())) {
                throw new BadRequestException("Duplicate participant name in group: " + trimmed);
            }
            cleanedNames.add(trimmed);
        }

        BillGroup group = BillGroup.builder()
                .name(request.getName().trim())
                .build();

        for (String name : cleanedNames) {
            Participant participant = Participant.builder()
                    .name(name)
                    .billGroup(group)
                    .build();
            group.getParticipants().add(participant);
        }

        BillGroup savedGroup = billGroupRepository.save(group);
        return mapToResponse(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long id) {
        BillGroup group = billGroupRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
        return mapToResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return billGroupRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillGroup findEntityById(Long id) {
        return billGroupRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
    }

    private GroupResponse mapToResponse(BillGroup group) {
        List<String> participantNames = group.getParticipants().stream()
                .map(Participant::getName)
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .participants(participantNames)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
