package com.kids.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Records a salary payout made to a teacher.
 *
 * One record per payment transaction — a month may have partial payments
 * (advances) and a final settlement, all linked to the same coveredMonth.
 */
@Entity
@Table(name = "teacher_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /** The calendar month this payment covers — stored as "YYYY-MM" */
    @Column(nullable = false, length = 7)
    private String coveredMonth;   // e.g. "2025-09"

    /** Gross calculated salary for the covered period */
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal grossAmount;

    /** Total deductions (absences, advances already paid, etc.) */
    @Column(precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    /** Net amount actually transferred / paid */
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    /** Reference number (bank transfer, receipt number, etc.) */
    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String notes;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public YearMonth getCoveredYearMonth() {
        return YearMonth.parse(coveredMonth);
    }

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum PaymentMethod {
        CASH,          // نقدي
        BANK_TRANSFER, // تحويل بنكي
        CHEQUE         // شيك
    }

    public enum PaymentStatus {
        PAID,    // مدفوع
        ADVANCE, // سلفة
        PENDING  // معلق
    }
}
