package com.kids.repositories;

import com.kids.entities.EmployeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {
}
