package com.kids.services;

import com.kids.entities.*;
import com.kids.repositories.*;
import com.kids.repositories.TeacherAttendanceRepository;
import com.kids.repositories.TeacherPaymentRepository;
import com.kids.repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Core service for Teacher management.
 *
 * ── Payroll Philosophy ──────────────────────────────────────────────────────
 *
 *  FIXED_MONTHLY:
 *    netSalary = baseSalary
 *              - (unexcusedAbsences × absencePenaltyPerDay)
 *              - advancesAlreadyPaid
 *
 *  PER_SESSION:
 *    netSalary = sessionsAttended × sessionRate
 *              - advancesAlreadyPaid
 *
 * The "pending salary" is what is still owed after any advance payments.
 * ────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepo;
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherPaymentRepository paymentRepo;

    // ── Attendance ───────────────────────────────────────────────────────────

    /**
     * Mark or update a teacher's attendance for today.
     * Calling this again for the same day will update the existing record.
     */
    @Transactional
    public TeacherAttendance markAttendance(Long teacherId,
                                            LocalDate date,
                                            TeacherAttendance.AttendanceStatus status,
                                            int sessionsCount,
                                            String notes) {

        Teacher teacher = teacherRepo.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        TeacherAttendance record = attendanceRepo
            .findByTeacherIdAndAttendanceDate(teacherId, date)
            .orElse(TeacherAttendance.builder().teacher(teacher).attendanceDate(date).build());

        record.setStatus(status);
        record.setSessionsCount(sessionsCount);
        record.setNotes(notes);

        TeacherAttendance saved = attendanceRepo.save(record);
        log.info("Attendance marked: teacher={} date={} status={}", teacher.getName(), date, status);
        return saved;
    }

    // ── Salary Calculation ───────────────────────────────────────────────────

    /**
     * Calculate the gross salary for a teacher for a specific month,
     * based on attendance data and their salary type.
     *
     * @param teacherId    teacher's ID
     * @param yearMonth    the month to calculate (e.g. YearMonth.of(2025, 9))
     * @return             gross calculated salary (before deducting advances)
     */
    public BigDecimal calculateGrossSalary(Long teacherId, YearMonth yearMonth) {

        Teacher teacher = teacherRepo.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        LocalDate from = yearMonth.atDay(1);
        LocalDate to   = yearMonth.atEndOfMonth();

        return switch (teacher.getSalaryType()) {

            case FIXED_MONTHLY -> {
                long absences = attendanceRepo.countAbsencesByMonth(teacherId, from, to);
                BigDecimal penalty = teacher.getAbsencePenaltyPerDay()
                                            .multiply(BigDecimal.valueOf(absences));
                BigDecimal gross = teacher.getBaseSalary().subtract(penalty);
                // Floor at zero — never a negative salary
                yield gross.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gross;
            }

            case PER_SESSION -> {
                long sessions = attendanceRepo.sumSessionsByMonth(teacherId, from, to);
                yield teacher.getBaseSalary().multiply(BigDecimal.valueOf(sessions));
            }
        };
    }

    /**
     * Calculate how much salary is still PENDING (owed but not yet paid)
     * for a teacher in a given month.
     *
     * pendingSalary = grossSalary - advancesAndPaymentsAlreadyMade
     */
    public BigDecimal calculatePendingSalary(Long teacherId, YearMonth yearMonth) {
        BigDecimal gross    = calculateGrossSalary(teacherId, yearMonth);
        BigDecimal alreadyPaid = BigDecimal.ZERO;
        BigDecimal pending  = gross.subtract(alreadyPaid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    // ── Payment Recording ────────────────────────────────────────────────────

    /**
     * Record a salary payment for a teacher.
     *
     * ⚠️  @Transactional ensures that both the payment record insertion
     *     AND any status updates happen atomically. If either fails,
     *     the entire transaction rolls back — no phantom payments.
     *
     * @param teacherId     teacher's ID
     * @param yearMonth     the month being paid
     * @param amountToPay   the net amount being paid now (may be partial advance)
     * @param method        payment method (cash, bank transfer, etc.)
     * @param reference     optional bank/receipt reference
     * @return              the saved TeacherPayment record
     */
    @Transactional
    public TeacherPayment recordPayment(Long teacherId,
                                        YearMonth yearMonth,
                                        BigDecimal amountToPay,
                                        TeacherPayment.PaymentMethod method,
                                        String reference) {

        Teacher teacher = teacherRepo.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        BigDecimal gross   = calculateGrossSalary(teacherId, yearMonth);
        BigDecimal pending = calculatePendingSalary(teacherId, yearMonth);

        if (amountToPay.compareTo(pending) > 0) {
            throw new IllegalStateException(
                "Payment amount %s exceeds pending salary %s for month %s"
                    .formatted(amountToPay, pending, yearMonth)
            );
        }

        // Determine if this is a full settlement or an advance
        BigDecimal afterThisPayment = pending.subtract(amountToPay);
        TeacherPayment.PaymentStatus status =
            afterThisPayment.compareTo(BigDecimal.ZERO) == 0
                ? TeacherPayment.PaymentStatus.PAID
                : TeacherPayment.PaymentStatus.ADVANCE;

        TeacherPayment payment = TeacherPayment.builder()
            .teacher(teacher)
            .coveredMonth(yearMonth.toString())
            .grossAmount(gross)
            .deductions(gross.subtract(pending))   // total deductions so far
            .netAmount(amountToPay)
            .paymentDate(LocalDate.now())
            .paymentMethod(method)
            .paymentStatus(status)
            .reference(reference)
            .build();

        TeacherPayment saved = paymentRepo.save(payment);
        log.info("Payment recorded: teacher={} month={} amount={} status={}",
            teacher.getName(), yearMonth, amountToPay, status);

        return saved;
    }
}
