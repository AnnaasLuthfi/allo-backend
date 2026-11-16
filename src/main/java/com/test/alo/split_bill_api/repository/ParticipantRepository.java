package com.test.alo.split_bill_api.repository;

import com.test.alo.split_bill_api.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByBillGroupId(Long billGroupId);

    Optional<Participant> findByBillGroupIdAndNameIgnoreCase(Long billGroupId, String name);
}
