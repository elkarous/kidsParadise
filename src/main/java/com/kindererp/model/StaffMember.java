package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fields shared by teachers and administrative employees (payroll and attendance work the same way). */
@MappedSuperclass
@Getter
@Setter
public abstract class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "hiring_date", nullable = false)
    private LocalDate hiringDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 20)
    private SalaryType salaryType = SalaryType.FIXED_MONTHLY;

    /** Monthly salary (FIXED_MONTHLY) or rate per session (PER_SESSION). */
    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    /** Deducted per unexcused absence day (FIXED_MONTHLY only). */
    @Column(name = "absence_penalty", nullable = false)
    private BigDecimal absencePenalty = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffStatus status = StaffStatus.ACTIVE;

    /** Specialty for a teacher, job title for an employee. */
    public abstract String getPosition();

    public abstract void setPosition(String position);

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && id != null && id.equals(((StaffMember) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
