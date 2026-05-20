package com.kids.services;

import com.kids.entities.Parent;
import org.springframework.stereotype.Service;


@Service
public class TuitionService {

    // المعلوم الشهري الأساسي للطالب الواحد (مثال: 100 دينار)
    private static final double BASE_MONTHLY_FEES = 100.0;

    /**
     * دالة لحساب القيمة الإجمالية المطلوبة من الولي شهرياً بعد احتساب التخفيض الآلي
     */
    public double calculateMonthlyFeesForParent(Parent parent) {
        if (parent == null || parent.getChildren() == null) {
            return 0.0;
        }

        int numberOfStudents = parent.getChildren().size();

        // إذا لم يكن لديه أبناء مسجلين
        if (numberOfStudents == 0) {
            return 0.0;
        }

        // 1. حساب القيمة الإجمالية قبل التخفيض
        double totalBeforeDiscount = numberOfStudents * BASE_MONTHLY_FEES;
        double discount = 0.0;

        // 2. تطبيق قاعدة التخفيض الخاصة بك
        if (numberOfStudents == 2) {
            discount = 5.0;  // تخفيض بـ 5 د إذا كان لديه طالبين
        } else if (numberOfStudents > 2) {
            discount = 10.0; // تخفيض بـ 10 د إذا كان لديه أكثر من طالبين
        }

        // 3. القيمة النهائية بعد الخصم
        return totalBeforeDiscount - discount;
    }
}