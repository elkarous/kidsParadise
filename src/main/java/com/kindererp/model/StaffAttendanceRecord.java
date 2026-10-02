package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/** One day of attendance for a staff member. */
@MappedSuperclass
@Getter
@Setter
public abstract class StaffAttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    /** Sessions worked that day (used by PER_SESSION payroll). */
    @Column(name = "sessions_count", nullable = false)
    private int sessionsCount = 1;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    @Column(length = 255)
    private String notes;
}
