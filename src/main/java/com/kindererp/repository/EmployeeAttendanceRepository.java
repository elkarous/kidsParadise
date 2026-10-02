package com.kindererp.repository;

import com.kindererp.model.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {

    List<EmployeeAttendance> findByAttendanceDate(LocalDate date);

    List<EmployeeAttendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate from, LocalDate to);

    boolean existsByEmployeeId(Long employeeId);
}
