package com.kids.services;

import com.kids.entities.*;
import com.kids.repositories.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * All business logic lives HERE in the service layer.
 * The JavaFX UI and future Angular/REST controllers are
 * pure consumers — they just call these methods and display results.
 * This means when you add a web dashboard later, you reuse
 * these exact methods — zero duplication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialReportingService {

    private final TeacherPaymentRepository teacherPaymentRepo;
    private final TeacherRepository        teacherRepo;
    private final TeacherService           teacherService;

    // ═══════════════════════════════════════════════════════════════════════════
    //  DTOs  (Records — immutable, no boilerplate, perfect for reports)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Monthly student revenue summary.
     * "How much did we collect? How much is still outstanding?"
     */
    public record StudentRevenueReport(
        YearMonth  month,
        BigDecimal totalRevenue,       // مجموع ما تم تحصيله
        BigDecimal outstandingBalance, // مجموع المتأخرات
        BigDecimal totalExpected,      // الإجمالي المتوقع
        int        studentsPaid,       // عدد الطلاب الذين دفعوا
        int        studentsWithBalance // عدد الطلاب بمتأخرات
    ) {
        /** Shortcut: collection rate as a percentage */
        public double collectionRate() {
            if (totalExpected.compareTo(BigDecimal.ZERO) == 0) return 100.0;
            return totalRevenue.doubleValue() / totalExpected.doubleValue() * 100.0;
        }
    }

    /**
     * Monthly teacher payroll summary.
     * "How much have we paid? How much do we still owe?"
     */
    public record TeacherPayrollReport(
        YearMonth  month,
        BigDecimal totalPaid,          // مجموع ما تم دفعه
        BigDecimal totalPending,       // مجموع المستحقات المتبقية
        BigDecimal totalGross,         // إجمالي الرواتب الصافية
        int        teachersFullyPaid,  // عدد المعلمين المدفوع لهم بالكامل
        int        teachersPending,    // عدد المعلمين بمستحقات متبقية
        List<TeacherSalaryLine> lines  // تفصيل لكل معلم
    ) {}

    /**
     * Per-teacher detail row in the payroll report.
     */
    public record TeacherSalaryLine(
        Long       teacherId,
        String     teacherName,
        SalaryType salaryType,
        BigDecimal grossSalary,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        boolean    fullyPaid
    ) {}

    /**
     * Combined dashboard snapshot — powers the two summary cards in JavaFX.
     */
    public record DashboardSnapshot(
        YearMonth  month,
        BigDecimal income,    // Card A — مدخول
        BigDecimal expenses,  // Card B — مصروف
        BigDecimal netProfit  // صافي الربح
    ) {
        public static DashboardSnapshot of(StudentRevenueReport sr, TeacherPayrollReport tr) {
            BigDecimal net = sr.totalRevenue().subtract(tr.totalPaid());
            return new DashboardSnapshot(sr.month(), sr.totalRevenue(), tr.totalPaid(), net);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Report Generation Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate the student revenue report for a given month.
     * Queries the student payment table for the month, then queries
     * outstanding balances from the enrollment table.
     */
    public StudentRevenueReport generateStudentReport(YearMonth month) {
        log.info("Generating student revenue report for {}", month);

        BigDecimal totalRevenue = teacherPaymentRepo
            .sumPaymentsForMonth(month.toString())
            .orElse(BigDecimal.ZERO);

        BigDecimal outstanding  = teacherPaymentRepo
            .sumOutstandingForMonth(month.toString())
            .orElse(BigDecimal.ZERO);

        int paid        = 0;
        int withBalance = 0;

        return new StudentRevenueReport(
            month,
            totalRevenue,
            outstanding,
            totalRevenue.add(outstanding),
            paid,
            withBalance
        );
    }

    /**
     * Generate the teacher payroll report for a given month.
     * Iterates active teachers, calculates gross and pending for each.
     */
    public TeacherPayrollReport generateTeacherReport(YearMonth month) {
        log.info("Generating teacher payroll report for {}", month);

        List<Teacher> activeTeachers = teacherRepo.findByStatus(Status.ACTIVE);

        List<TeacherSalaryLine> lines = activeTeachers.stream()
            .map(t -> buildSalaryLine(t, month))
            .toList();

        BigDecimal totalPaid    = lines.stream().map(TeacherSalaryLine::amountPaid)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = lines.stream().map(TeacherSalaryLine::pendingAmount)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGross   = lines.stream().map(TeacherSalaryLine::grossSalary)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add);

        long fullyPaid = lines.stream().filter(TeacherSalaryLine::fullyPaid).count();
        long pending   = lines.stream().filter(l -> !l.fullyPaid()).count();

        return new TeacherPayrollReport(
            month, totalPaid, totalPending, totalGross,
            (int) fullyPaid, (int) pending, lines
        );
    }

    /** Combined snapshot for the dashboard cards */
    public DashboardSnapshot getDashboardSnapshot(YearMonth month) {
        return DashboardSnapshot.of(
            generateStudentReport(month),
            generateTeacherReport(month)
        );
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private TeacherSalaryLine buildSalaryLine(Teacher teacher, YearMonth month) {
        BigDecimal gross   = teacherService.calculateGrossSalary(teacher.getId(), month);
        BigDecimal pending = teacherService.calculatePendingSalary(teacher.getId(), month);
        BigDecimal paid    = gross.subtract(pending);

        return new TeacherSalaryLine(
            teacher.getId(),
            teacher.getName(),
            teacher.getSalaryType(),
            gross,
            paid,
            pending,
            pending.compareTo(BigDecimal.ZERO) == 0
        );
    }
}
