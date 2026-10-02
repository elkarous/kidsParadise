package com.kindererp.service.dto;

import com.kindererp.model.SalaryType;

import java.math.BigDecimal;

/** Salary situation of one staff member for one working month. */
public record PayrollRow(Long staffId, String name, String position, SalaryType salaryType,
                         int absences, int sessions,
                         BigDecimal gross, BigDecimal paid, BigDecimal pending) {

    public boolean fullyPaid() {
        return pending.signum() == 0;
    }

    public boolean partiallyPaid() {
        return paid.signum() > 0 && pending.signum() > 0;
    }
}
