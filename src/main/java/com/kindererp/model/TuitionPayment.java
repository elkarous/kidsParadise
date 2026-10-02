package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Monthly tuition paid by a family for all its enrolled children. */
@Entity
@Table(name = "tuition_payments")
@Getter
@Setter
@NoArgsConstructor
public class TuitionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "working_month_id", nullable = false)
    private WorkingMonth workingMonth;

    @Column(name = "children_count", nullable = false)
    private int childrenCount;

    /** Amount actually paid, after discount. */
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(length = 255)
    private String notes;
}
