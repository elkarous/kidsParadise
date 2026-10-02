package com.kindererp.repository;

import com.kindererp.model.EmployeePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {

    List<EmployeePayment> findByEmployeeIdAndWorkingMonthId(Long employeeId, Long workingMonthId);

    boolean existsByEmployeeId(Long employeeId);
}
