package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A salary payout to a staff member. A month can have several payouts (advances, then a final
 * settlement), all linked to the same working month.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class StaffPaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "working_month_id", nullable = false)
    private WorkingMonth workingMonth;

    /** Salary due for the month at the time of payment. */
    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    /** Amount paid by this payout. */
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PAID;

    @Column(length = 100)
    private String reference;

    @Column(length = 255)
    private String notes;
}
