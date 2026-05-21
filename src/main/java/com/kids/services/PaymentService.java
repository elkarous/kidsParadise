package com.kids.services;

import com.kids.entities.Parent;
import com.kids.entities.Payment;
import com.kids.entities.WorkingMonth;
import com.kids.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TuitionService tuitionService; // الخدمة المسؤولة عن حساب الخصومات والأقساط

    /**
     * جلب كافة السجلات المالية لعرضها بالجدول
     */
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    /**
     * تسجيل دفعة شهرية جديدة لولي أمر مرتبطة هيكلياً بكائن الشهر الدراسي
     */
    @Transactional
    public Payment registerMonthlyPayment(Parent parent, WorkingMonth workingMonth, String paymentMethod, String notes) {

        // 1. التحقق من المدخلات الأساسية لحماية النظام من الـ Null Pointer Exception
        if (parent == null || workingMonth == null) {
            throw new IllegalArgumentException("يجب تحديد ولي الأمر والشهر الدراسي المستهدف بدقة!");
        }

        // 2. 🔗 التحقق المحدث: التأكد من أن الولي لم يدفع لهذا الشهر الدراسي مسبقاً (مقارنة بالـ ID أو بالكائن)
        if (paymentRepository.existsByParentIdAndWorkingMonthId(parent.getId(), workingMonth.getId())) {
            throw new IllegalStateException("هذا الولي قام بدفع معلوم الشهر الدراسي (" + workingMonth.getMonthName() + ") مسبقاً!");
        }

        // 3. التحقق من حالة الشهر الدراسي (ميزة إضافية لحماية الحسابات)
        if (workingMonth.isClosed()) {
            throw new IllegalStateException("لا يمكن تسجيل دفعات لشهر دراسي مغلق حسابياً!");
        }

        // 4. حساب المبلغ المطلوب والتخفيض بناءً على لوجيك المدرسة الحالي
        // تم استبدال getChildren بـ getStudents حسب الهيكلية المعتادة للـ Entities لديك
        int studentCount = (parent.getChildren() != null) ? parent.getChildren().size() : 0;
        double baseFees = studentCount * 100.0; // 100 دينار للطالب الواحد كمثال
        double finalFees = tuitionService.calculateMonthlyFeesForParent(parent);
        double discount = baseFees - finalFees;

        // 5. بناء كائن الدفعة الجديد المتوافق مع الـ Entity المحدثة
        Payment payment = new Payment();
        payment.setParent(parent);
        payment.setWorkingMonth(workingMonth); // 🌟 حقن كائن الشهر الدراسي الكامل كـ Foreign Key
        payment.setAmountPaid(finalFees);      // حفظ المبلغ الصافي المدفوع
        payment.setDiscountApplied(discount);  // توثيق قيمة الخصم المعطاة تاريخياً
        payment.setPaymentDate(LocalDate.now()); // تاريخ اليوم التلقائي للحركة المالية
        payment.setPaymentMethod(paymentMethod);
        payment.setNotes(notes);

        // 6. الحفظ في قاعدة البيانات
        return paymentRepository.save(payment);
    }

    /**
     * فحص حركي لحالة الدفع الخاصة بولي أمر لشهر معين
     */
    public boolean isMonthPaid(Long parentId, Long workingMonthId) {
        if (parentId == null || workingMonthId == null) return false;
        return paymentRepository.existsByParentIdAndWorkingMonthId(parentId, workingMonthId);
    }
}