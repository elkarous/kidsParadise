package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "attendance_records")
@Data
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // السجل يتبع لحصة معينة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SchoolSession session;

    // السجل يخص طالباً معيناً
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status; // PRESENT, ABSENT, LATE

    private String remarks; // ملاحظات مثل: "غياب مبرر"

    // Getters, Setters, Constructors
}

enum AttendanceStatus {
    PRESENT, ABSENT, LATE
}
