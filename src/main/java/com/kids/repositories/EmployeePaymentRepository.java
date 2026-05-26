package com.kids.repositories;

import com.kids.entities.EmployeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {
    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM EmployeePayment p WHERE p.employee.id = :employeeId AND p.workingMonth.id = :workingMonthId")
    BigDecimal sumPaidAmountByMonth(@Param("employeeId") Long employeeId, @Param("workingMonthId") Long workingMonthId);
}
