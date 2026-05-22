package com.kids.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an administrator or staff member in the Garden School system.
 * Supports standard administrative roles and tracking.
 */
@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString // لومبوك سيقوم بإنشاء دالة toString تلقائياً
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full name — stored in UTF-8, supports Arabic */
    @Column(nullable = false, length = 200)
    private String name;

    /** e.g. "Director / مدير", "Accountant / محاسب", "Receptionist / استقبال" */
    @Column(nullable = false, length = 100)
    private String role;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private LocalDate hiringDate;

    /** Fixed monthly gross before deductions */
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal baseSalary;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AdminStatus status = AdminStatus.ACTIVE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;



    // ── Enums ────────────────────────────────────────────────────────────────

    public enum AdminStatus {
        ACTIVE,   // نشط
        INACTIVE, // غير نشط
        ON_LEAVE  // في إجازة
    }
}