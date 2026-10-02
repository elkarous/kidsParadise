package com.kindererp.service;

import com.kindererp.model.*;
import com.kindererp.repository.*;
import com.kindererp.service.dto.PayrollRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.kindererp.service.Checks.require;
import static com.kindererp.service.Checks.trimToNull;

/**
 * Monthly salaries of teachers and employees.
 * <ul>
 *   <li>FIXED_MONTHLY: base salary minus (unexcused absences x absence penalty), never below zero</li>
 *   <li>PER_SESSION: rate x sessions worked on days marked present or late</li>
 * </ul>
 * A month can be paid in several payouts; the total paid can never exceed the salary due.
 */
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final StaffService staffService;
    private final StaffAttendanceService attendanceService;
    private final WorkingMonthRepository monthRepository;
    private final TeacherRepository teacherRepository;
    private final EmployeeRepository employeeRepository;
    private final TeacherPaymentRepository teacherPaymentRepository;
    private final EmployeePaymentRepository employeePaymentRepository;

    public static BigDecimal grossSalary(StaffMember member, List<? extends StaffAttendanceRecord> monthRecords) {
        return switch (member.getSalaryType()) {
            case FIXED_MONTHLY -> {
                long absences = countAbsences(monthRecords);
                BigDecimal penalty = member.getAbsencePenalty().multiply(BigDecimal.valueOf(absences));
                yield member.getBaseSalary().subtract(penalty).max(BigDecimal.ZERO);
            }
            case PER_SESSION -> member.getBaseSalary().multiply(BigDecimal.valueOf(countSessions(monthRecords)));
        };
    }

    @Transactional(readOnly = true)
    public List<PayrollRow> monthPayroll(StaffKind kind, Long workingMonthId) {
        WorkingMonth month = month(workingMonthId);
        List<PayrollRow> rows = new ArrayList<>();
        for (StaffMember member : staffService.findAll(kind)) {
            PayrollRow row = row(kind, member, month);
            if (member.getStatus() == StaffStatus.ACTIVE || row.paid().signum() > 0) {
                rows.add(row);
            }
        }
        return rows;
    }

    @Transactional
    public StaffPaymentRecord pay(StaffKind kind, Long staffId, Long workingMonthId, BigDecimal amount,
                                  PaymentMethod method, String reference) {
        WorkingMonth month = month(workingMonthId);
        require(!month.isClosed(), "payments.error.monthClosed");
        require(amount != null && amount.signum() > 0, "payroll.error.amountPositive");

        StaffMember member = kind == StaffKind.TEACHER
                ? teacherRepository.findById(staffId).orElseThrow(() -> new BusinessException("error.notFound"))
                : employeeRepository.findById(staffId).orElseThrow(() -> new BusinessException("error.notFound"));
        PayrollRow row = row(kind, member, month);
        require(row.pending().signum() > 0, "payroll.error.alreadyPaid", member.getName());
        require(amount.compareTo(row.pending()) <= 0, "payroll.error.exceedsPending", row.pending().toPlainString());

        StaffPaymentRecord payment;
        if (member instanceof Teacher teacher) {
            TeacherPayment teacherPayment = new TeacherPayment();
            teacherPayment.setTeacher(teacher);
            payment = teacherPayment;
        } else {
            EmployeePayment employeePayment = new EmployeePayment();
            employeePayment.setEmployee((Employee) member);
            payment = employeePayment;
        }
        payment.setWorkingMonth(month);
        payment.setGrossAmount(row.gross());
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setMethod(method == null ? PaymentMethod.CASH : method);
        payment.setStatus(amount.compareTo(row.pending()) == 0 ? PaymentStatus.PAID : PaymentStatus.ADVANCE);
        payment.setReference(trimToNull(reference));

        return payment instanceof TeacherPayment tp ? teacherPaymentRepository.save(tp)
                : employeePaymentRepository.save((EmployeePayment) payment);
    }

    private PayrollRow row(StaffKind kind, StaffMember member, WorkingMonth month) {
        List<? extends StaffAttendanceRecord> records =
                attendanceService.records(kind, member.getId(), month.firstDay(), month.lastDay());
        BigDecimal gross = grossSalary(member, records);
        BigDecimal paid = payments(kind, member.getId(), month.getId()).stream()
                .map(StaffPaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollRow(member.getId(), member.getName(), member.getPosition(), member.getSalaryType(),
                (int) countAbsences(records), (int) countSessions(records),
                gross, paid, gross.subtract(paid).max(BigDecimal.ZERO));
    }

    private List<? extends StaffPaymentRecord> payments(StaffKind kind, Long staffId, Long monthId) {
        return kind == StaffKind.TEACHER
                ? teacherPaymentRepository.findByTeacherIdAndWorkingMonthId(staffId, monthId)
                : employeePaymentRepository.findByEmployeeIdAndWorkingMonthId(staffId, monthId);
    }

    private WorkingMonth month(Long id) {
        return monthRepository.findById(id).orElseThrow(() -> new BusinessException("error.notFound"));
    }

    private static long countAbsences(List<? extends StaffAttendanceRecord> records) {
        return records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
    }

    private static long countSessions(List<? extends StaffAttendanceRecord> records) {
        return records.stream().filter(r -> r.getStatus().isPresent()).mapToLong(StaffAttendanceRecord::getSessionsCount).sum();
    }
}
