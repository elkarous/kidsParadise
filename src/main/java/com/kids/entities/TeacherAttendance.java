package com.kids.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Daily attendance record for a teacher.
 *
 * Status matrix:
 *   PRESENT   → full day counted
 *   LATE      → counted as present but may trigger partial deduction (configurable)
 *   ABSENT    → deduction applied (FIXED) or session not counted (PER_SESSION)
 *   EXCUSED   → absent but no financial penalty
 */
@Entity
@Table(
    name = "teacher_attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "attendanceDate"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    /** Number of sessions held on this day (relevant for PER_SESSION payroll) */
    @Builder.Default
    private int sessionsCount = 1;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    /** Free-text notes (supports Arabic) */
    @Column(length = 500)
    private String notes;
}
