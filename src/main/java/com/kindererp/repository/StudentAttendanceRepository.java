package com.kindererp.repository;

import com.kindererp.model.AttendanceStatus;
import com.kindererp.model.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    List<StudentAttendance> findBySchoolClassIdAndAttendanceDate(Long classId, LocalDate date);

    long countByStudentIdAndStatusAndAttendanceDateBetween(Long studentId, AttendanceStatus status,
                                                          LocalDate from, LocalDate to);

    boolean existsBySchoolClassId(Long classId);
}
