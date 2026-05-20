package com.kids.services;

import com.kids.entities.Parent;
import com.kids.entities.Payment;
import com.kids.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TuitionService tuitionService; // الخدمة التي صممناها لحساب الخصم

    /**
     * تسجيل دفعة شهرية جديدة لولي أمر
     */
    @Transactional
    public Payment registerMonthlyPayment(Parent parent, String targetMonth, String paymentMethod, String notes) {

        // 1. التأكد من أن الولي لم يدفع لهذا الشهر مسبقاً
        if (paymentRepository.existsByParentIdAndTargetMonth(parent.getId(), targetMonth)) {
            throw new IllegalStateException("هذا الولي قام بدفع معلوم هذا الشهر مسبقاً!");
        }

        // 2. حساب المبلغ المطلوب والتخفيض بناءً على عدد الأبناء الحالي
        int studentCount = parent.getChildren() != null ? parent.getChildren().size() : 0;
        double baseFees = studentCount * 100.0; // 100 د للطالب الواحد كمثال
        double finalFees = tuitionService.calculateMonthlyFeesForParent(parent);
        double discount = baseFees - finalFees;

        // 3. بناء كائن الدفعة الجديد
        Payment payment = new Payment();
        payment.setParent(parent);
        payment.setAmountPaid(finalFees); // حفظ المبلغ الصافي المدفوع
        payment.setDiscountApplied(discount); // توثيق قيمة الخصم المعطاة
        payment.setPaymentDate(LocalDate.now()); // تاريخ اليوم تلقائياً
        payment.setTargetMonth(targetMonth); // الشهر المستهدف (مثال: "2026-05")
        payment.setPaymentMethod(paymentMethod);
        payment.setNotes(notes);

        // 4. الحفظ في قاعدة البيانات
        return paymentRepository.save(payment);
    }
    public boolean isMonthPaid(Long parentId, String targetMonth) {
        return paymentRepository.existsByParentIdAndTargetMonth(parentId, targetMonth);
    }
}