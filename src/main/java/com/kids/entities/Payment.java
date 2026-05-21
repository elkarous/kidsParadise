package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. ربط الدفعة بولي الأمر المسؤول عن الدفع (علاقة متعدد لواحد)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    // 2. 🌟 الربط الهيكلي الجديد: كل حركة دفع مرتبطة بكائن الشهر الشغال بالكامل
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "working_month_id", nullable = false)
    private WorkingMonth workingMonth;

    // المبلغ الذي دفعه الولي فعلياً في هذه الحركة
    @Column(nullable = false)
    private double amountPaid;

    // قيمة التخفيض التي طُبقت عليه عند الدفع
    private double discountApplied;

    // تاريخ عملية الدفع الفعلي
    @Column(nullable = false)
    private LocalDate paymentDate;

    // طريقة الدفع (نقدًا، صك بنكي، تحويل...)
    private String paymentMethod;

    // ملاحظات إضافية (مثل رقم الصك أو اسم المستلم)
    private String notes;
}