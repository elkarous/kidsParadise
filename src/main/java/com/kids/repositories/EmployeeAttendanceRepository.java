package com.kids.repositories;

import com.kids.entities.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {
    @Query("SELECT COUNT(a) FROM EmployeeAttendance a WHERE a.employee.id = :teacherId" +
            " AND a.attendanceDate >= :from AND a.attendanceDate <= :to" +
            " AND a.status = 'ABSENT'")
    long countAbsencesByMonth(
            @Param("teacherId") Long teacherId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );


    /** Sum sessions for PER_SESSION payroll */
    @Query(" SELECT COALESCE(SUM(a.sessionsCount), 0) FROM EmployeeAttendance a" +
            " WHERE a.employee.id = :teacherId" +
            " AND a.attendanceDate >= :from" +
            " AND a.attendanceDate <= :to" +
            " AND a.status IN ('PRESENT','LATE')")
    long sumSessionsByMonth(
            @Param("teacherId") Long teacherId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
