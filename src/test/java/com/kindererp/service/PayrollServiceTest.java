package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.*;
import com.kindererp.service.dto.PayrollRow;
import com.kindererp.service.dto.StaffAttendanceRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayrollServiceTest extends IntegrationTest {

    @Autowired
    private PayrollService payrollService;
    @Autowired
    private StaffAttendanceService attendanceService;

    private WorkingMonth october;

    @BeforeEach
    void setUp() {
        settings();
        october = schoolYearService.months(year().getId()).stream()
                .filter(m -> m.getCalendarMonth() == 10).findFirst().orElseThrow();
    }

    private void mark(Teacher teacher, int day, AttendanceStatus status, int sessions) {
        LocalDate date = LocalDate.of(2025, 10, day);
        List<StaffAttendanceRow> rows = attendanceService.day(StaffKind.TEACHER, date);
        rows.stream().filter(r -> r.getStaffId().equals(teacher.getId())).forEach(r -> {
            r.setStatus(status);
            r.setSessions(sessions);
        });
        attendanceService.saveDay(StaffKind.TEACHER, date, rows);
    }

    private PayrollRow row(Teacher teacher) {
        return payrollService.monthPayroll(StaffKind.TEACHER, october.getId()).stream()
                .filter(r -> r.staffId().equals(teacher.getId())).findFirst().orElseThrow();
    }

    @Test
    void fixedSalaryLosesThePenaltyForEachUnexcusedAbsence() {
        Teacher teacher = teacher(SalaryType.FIXED_MONTHLY, "1000", "40");
        mark(teacher, 6, AttendanceStatus.ABSENT, 0);
        mark(teacher, 7, AttendanceStatus.ABSENT, 0);
        mark(teacher, 8, AttendanceStatus.EXCUSED, 0);
        mark(teacher, 9, AttendanceStatus.LATE, 1);

        PayrollRow row = row(teacher);
        assertThat(row.absences()).isEqualTo(2);
        assertThat(row.gross()).isEqualByComparingTo("920");
        assertThat(row.pending()).isEqualByComparingTo("920");
    }

    @Test
    void perSessionSalaryCountsSessionsOnPresentAndLateDays() {
        Teacher teacher = teacher(SalaryType.PER_SESSION, "25", "0");
        mark(teacher, 6, AttendanceStatus.PRESENT, 2);
        mark(teacher, 7, AttendanceStatus.LATE, 1);
        mark(teacher, 8, AttendanceStatus.ABSENT, 3);

        PayrollRow row = row(teacher);
        assertThat(row.sessions()).isEqualTo(3);
        assertThat(row.gross()).isEqualByComparingTo("75");
    }

    @Test
    void anAdvanceThenTheBalanceSettlesTheMonth() {
        Teacher teacher = teacher(SalaryType.FIXED_MONTHLY, "1000", "0");

        StaffPaymentRecord advance = payrollService.pay(StaffKind.TEACHER, teacher.getId(), october.getId(),
                new BigDecimal("300"), PaymentMethod.CASH, null);
        assertThat(advance.getStatus()).isEqualTo(PaymentStatus.ADVANCE);
        assertThat(row(teacher).pending()).isEqualByComparingTo("700");

        StaffPaymentRecord balance = payrollService.pay(StaffKind.TEACHER, teacher.getId(), october.getId(),
                new BigDecimal("700"), PaymentMethod.BANK_TRANSFER, "VIR-1");
        assertThat(balance.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(row(teacher).fullyPaid()).isTrue();

        assertThatThrownBy(() -> payrollService.pay(StaffKind.TEACHER, teacher.getId(), october.getId(),
                BigDecimal.ONE, PaymentMethod.CASH, null)).hasMessage("payroll.error.alreadyPaid");
    }

    @Test
    void refusesToPayMoreThanWhatIsDue() {
        Teacher teacher = teacher(SalaryType.FIXED_MONTHLY, "500", "0");
        assertThatThrownBy(() -> payrollService.pay(StaffKind.TEACHER, teacher.getId(), october.getId(),
                new BigDecimal("500.001"), PaymentMethod.CASH, null)).hasMessage("payroll.error.exceedsPending");
    }

    @Test
    void refusesZeroOrNegativeAmounts() {
        Teacher teacher = teacher(SalaryType.FIXED_MONTHLY, "500", "0");
        assertThatThrownBy(() -> payrollService.pay(StaffKind.TEACHER, teacher.getId(), october.getId(),
                BigDecimal.ZERO, PaymentMethod.CASH, null)).hasMessage("payroll.error.amountPositive");
    }
}
