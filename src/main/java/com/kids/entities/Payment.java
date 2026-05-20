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

    // ربط الدفعة بولي الأمر المسؤول عن الدفع
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    // المبلغ الذي دفعه الولي فعلياً في هذه الحركة
    @Column(nullable = false)
    private double amountPaid;

    // قيمة التخفيض التي طُبقت عليه عند الدفع (لحفظ التاريخ المالي حتى لو تغير عدد الأبناء لاحقاً)
    private double discountApplied;

    // تاريخ عملية الدفع الفعلي (مثلاً: اليوم 19-05-2026)
    @Column(nullable = false)
    private LocalDate paymentDate;

    // الشهر والسنة المستهدفة من الدفع (مثلاً: "2026-05" لشهريّة ماي)
    @Column(nullable = false)
    private String targetMonth; // صيغة YYYY-MM تسهل الفلترة جداً

    // طريقة الدفع (نقدًا، صك بنكي، تحويل...)
    private String paymentMethod;

    // ملاحظات إضافية (مثل رقم الصك أو اسم المستلم)
    private String notes;
}