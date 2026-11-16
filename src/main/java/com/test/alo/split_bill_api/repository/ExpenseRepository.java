package com.test.alo.split_bill_api.repository;

import com.test.alo.split_bill_api.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT DISTINCT e FROM Expense e " +
            "LEFT JOIN FETCH e.paidBy " +
            "LEFT JOIN FETCH e.shares s " +
            "LEFT JOIN FETCH s.participant " +
            "WHERE e.billGroup.id = :billGroupId ORDER BY e.createdAt ASC")
    List<Expense> findByBillGroupIdWithDetails(@Param("billGroupId") Long billGroupId);
}
