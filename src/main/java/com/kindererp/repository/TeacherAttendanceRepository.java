package com.kindererp.repository;

import com.kindererp.model.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

    List<TeacherAttendance> findByAttendanceDate(LocalDate date);

    List<TeacherAttendance> findByTeacherIdAndAttendanceDateBetween(Long teacherId, LocalDate from, LocalDate to);

    boolean existsByTeacherId(Long teacherId);
}
