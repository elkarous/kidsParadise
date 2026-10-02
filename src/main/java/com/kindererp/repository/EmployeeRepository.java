package com.kindererp.repository;

import com.kindererp.model.Employee;
import com.kindererp.model.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByOrderByNameAsc();

    List<Employee> findByStatusOrderByNameAsc(StaffStatus status);
}
