package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_sessions")
@Data
public class SchoolSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate sessionDate; // تاريخ الحصة
    private LocalTime startTime;   // وقت البدء
    private LocalTime endTime;     // وقت الانتهاء

    // العديد من الحصص تتبع لشهر عمل واحد
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "working_month_id", nullable = false)
    private WorkingMonth workingMonth;

    // الحصة يدرسها معلم واحد (من كلاس Teacher)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // الحصة تكون لصف معين (من كلاس SchoolClass الذي عملنا عليه سابقاً)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    // دفتر الحضور الخاص بالحصة
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();

    // Getters, Setters, Constructors
}