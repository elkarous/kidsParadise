package com.kids.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a teacher in the Garden School system.
 * Supports two payroll models: Fixed Monthly or Per Session.
 * Salary calculation:
 *   FIXED:BaseSalary - (absences × absencePenaltyPerDay)
 *   PER_SESSION: sessionRate × sessionsAttended
 */
@Entity
@Table(name = "teachers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full name — stored in UTF-8, supports Arabic */
    @Column(nullable = false, length = 200)
    private String name;

    /** e.g. "Math / رياضيات", "Arabic / عربي" */
    @Column(length = 100)
    private String specialty;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private LocalDate hiringDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;

    /**
     * FIXED  → monthly gross before deductions
     * PER_SESSION → rate per individual session
     */
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal baseSalary;

    /**
     * Amount deducted per absent working day (FIXED salary type only).
     * Default = 0 if the school does not apply deductions.
     */
    @Column(precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal absencePenaltyPerDay = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TeacherStatus status = TeacherStatus.ACTIVE;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private List<SchoolClass> schoolClasses;
    // ── Enums ────────────────────────────────────────────────────────────────

    public enum SalaryType {
        FIXED_MONTHLY,   // ثابت شهري
        PER_SESSION      // بالجلسة
    }

    public enum TeacherStatus {
        ACTIVE,   // نشط
        INACTIVE, // غير نشط
        ON_LEAVE  // في إجازة
    }
}
