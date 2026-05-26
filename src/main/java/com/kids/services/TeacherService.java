package com.kids.services;

import com.kids.entities.*;
import com.kids.repositories.TeacherAttendanceRepository;
import com.kids.repositories.TeacherPaymentRepository;
import com.kids.repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Core service for Teacher management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepo;
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherPaymentRepository paymentRepo;

    // ── Attendance ───────────────────────────────────────────────────────────

    @Transactional
    public TeacherAttendance markAttendance(Long teacherId,
                                            LocalDate date,
                                            AttendanceStatus status,
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
     * 📊 حساب الراتب الإجمالي للأستاذ بناءً على بيانات الشهر الدراسي النشط WorkingMonth
     */
    public BigDecimal calculateGrossSalary(Long teacherId, WorkingMonth workingMonth) {
        if (workingMonth == null) {
            throw new IllegalArgumentException("Working month cannot be null");
        }

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        // تحويل أرقام الشهر الدراسي إلى تواريخ محلية لمعرفة أيام العمل والحصص
        YearMonth yearMonth = YearMonth.of(workingMonth.getYear(), workingMonth.getMonthNumber());
        LocalDate from = yearMonth.atDay(1);
        LocalDate to   = yearMonth.atEndOfMonth();

        return switch (teacher.getSalaryType()) {
            case FIXED_MONTHLY -> {
                long absences = attendanceRepo.countAbsencesByMonth(teacherId, from, to);
                BigDecimal penalty = teacher.getAbsencePenaltyPerDay()
                        .multiply(BigDecimal.valueOf(absences));
                BigDecimal gross = teacher.getBaseSalary().subtract(penalty);
                yield gross.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gross;
            }
            case PER_SESSION -> {
                long sessions = attendanceRepo.sumSessionsByMonth(teacherId, from, to);
                yield teacher.getBaseSalary().multiply(BigDecimal.valueOf(sessions));
            }
        };
    }

    /**
     * 💰 حساب المبلغ المتبقي المستحق للأستاذ بعد خصم كافة التسبيقات السابقة للشهر الدراسي
     */
    public BigDecimal calculatePendingSalary(Long teacherId, WorkingMonth workingMonth) {
        if (workingMonth == null) return BigDecimal.ZERO;

        BigDecimal gross = calculateGrossSalary(teacherId, workingMonth);

        // الاستعلام الفعلي من قاعدة البيانات باستخدام المعرف الفريد للـ WorkingMonth
        BigDecimal alreadyPaid = paymentRepo.sumPaidAmountByMonth(teacherId, workingMonth.getId());
        if (alreadyPaid == null) {
            alreadyPaid = BigDecimal.ZERO;
        }

        BigDecimal pending = gross.subtract(alreadyPaid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    // ── Payment Recording ────────────────────────────────────────────────────

    /**
     * 💳 تسجيل حركة صرف مستحقات ماليّة للأستاذ وربطها بهيكل الشهر الدراسي
     */
    @Transactional
    public TeacherPayment recordPayment(Long teacherId,
                                        WorkingMonth workingMonth,
                                        BigDecimal amountToPay,
                                        PaymentMethod method,
                                        String reference) {

        if (workingMonth == null) {
            throw new IllegalArgumentException("Working month cannot be null");
        }

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        BigDecimal gross   = calculateGrossSalary(teacherId, workingMonth);
        BigDecimal pending = calculatePendingSalary(teacherId, workingMonth);

        if (amountToPay.compareTo(pending) > 0) {
            throw new IllegalStateException(
                    "Payment amount %s exceeds pending salary %s for month %s"
                            .formatted(amountToPay, pending, workingMonth.getMonthName())
            );
        }

        BigDecimal afterThisPayment = pending.subtract(amountToPay);
        PaymentStatus status = afterThisPayment.compareTo(BigDecimal.ZERO) == 0
                ? PaymentStatus.PAID
                : PaymentStatus.ADVANCE;

        // 🟢 بناء كائن حركة الدفع وربطه بـ workingMonth بشكل صحيح بالكامل
        TeacherPayment payment = TeacherPayment.builder()
                .teacher(teacher)
                .workingMonth(workingMonth)
                .grossAmount(gross)
                .deductions(gross.subtract(pending)) // مجموع الاقتطاعات أو الدفوعات السابقة حتى اللحظة
                .netAmount(amountToPay)
                .paymentDate(LocalDate.now())
                .paymentMethod(method)
                .paymentStatus(status)
                .reference(reference)
                .build();

        TeacherPayment saved = paymentRepo.save(payment);
        log.info("Payment recorded: teacher={} month={} amount={} status={}",
                teacher.getName(), workingMonth.getMonthName(), amountToPay, status);

        return saved;
    }

    // ── المساعدات وبقية الدوال المستقرة ──────────────────────────────────────────

    public List<Teacher> findAllActive() {
        return this.teacherRepo.findByStatus(Status.ACTIVE);
    }

    public List<Teacher> findAll() {
        return this.teacherRepo.findAll();
    }

    public void save(Teacher currentStaff) {
        this.teacherRepo.save(currentStaff);
    }

    public void deleteById(Long id) {
        this.teacherRepo.deleteById(id);
    }

    public Optional<TeacherAttendance> findAttendanceByDateAndId(Long id, LocalDate selectedDate) {
        return this.attendanceRepo.findByTeacherIdAndAttendanceDate(id, selectedDate);
    }

    @Transactional
    public void saveAttendance(Teacher teacher, LocalDate date, AttendanceStatus status, LocalTime checkIn, LocalTime checkOut, String notes) {
        TeacherAttendance attendance = attendanceRepo
                .findByTeacherIdAndAttendanceDate(teacher.getId(), date)
                .orElseGet(() -> TeacherAttendance.builder()
                        .teacher(teacher)
                        .attendanceDate(date)
                        .build());

        attendance.setStatus(status);
        attendance.setCheckInTime(checkIn);
        attendance.setCheckOutTime(checkOut);
        attendance.setNotes(notes);

        attendanceRepo.save(attendance);
    }
}