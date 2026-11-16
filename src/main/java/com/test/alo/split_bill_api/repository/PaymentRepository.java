package com.test.alo.split_bill_api.repository;

import com.test.alo.split_bill_api.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.paidBy " +
            "LEFT JOIN FETCH p.paidTo " +
            "WHERE p.billGroup.id = :billGroupId ORDER BY p.createdAt ASC")
    List<Payment> findByBillGroupIdWithDetails(@Param("billGroupId") Long billGroupId);
}
