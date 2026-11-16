package com.test.alo.split_bill_api.repository;

import com.test.alo.split_bill_api.model.BillGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {

    @EntityGraph(attributePaths = { "participants" })
    Optional<BillGroup> findWithParticipantsById(Long id);
}
