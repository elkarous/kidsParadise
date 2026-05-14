package com.kids.repositories;


// ─── TeacherAttendanceRepository ─────────────────────────────────────────────

import com.kids.entities.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

    Optional<TeacherAttendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate date);

    /** All attendance records for a teacher within a given month */
    @Query("""
        SELECT a FROM TeacherAttendance a
        WHERE a.teacher.id = :teacherId
          AND a.attendanceDate >= :from
          AND a.attendanceDate <= :to
        ORDER BY a.attendanceDate
    """)
    List<TeacherAttendance> findByTeacherAndMonth(
        @Param("teacherId") Long teacherId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /** Count ABSENT (non-excused) days in a month */
    @Query("""
        SELECT COUNT(a) FROM TeacherAttendance a
        WHERE a.teacher.id = :teacherId
          AND a.attendanceDate >= :from
          AND a.attendanceDate <= :to
          AND a.status = 'ABSENT'
    """)
    long countAbsencesByMonth(
        @Param("teacherId") Long teacherId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /** Sum sessions for PER_SESSION payroll */
    @Query("""
        SELECT COALESCE(SUM(a.sessionsCount), 0) FROM TeacherAttendance a
        WHERE a.teacher.id = :teacherId
          AND a.attendanceDate >= :from
          AND a.attendanceDate <= :to
          AND a.status IN ('PRESENT','LATE')
    """)
    long sumSessionsByMonth(
        @Param("teacherId") Long teacherId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}

// ─── TeacherPaymentRepository ─────────────────────────────────────────────────

