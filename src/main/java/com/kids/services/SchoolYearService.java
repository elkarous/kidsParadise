package com.kids.services;

import com.kids.entities.SchoolYear;
import com.kids.repositories.SchoolYearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SchoolYearService {

    @Autowired
    private SchoolYearRepository schoolYearRepository;

    // 1. جلب كل السنوات لملء القائمة المنسدلة تحت نموذج تسجيل الدخول
    public List<SchoolYear> getAllYears() {
        return schoolYearRepository.findAll();
    }

    // 2. جلب السنة الحالية تلقائياً بناءً على تاريخ اليوم الفعلي
    public SchoolYear findActualYear() {
        LocalDate today = LocalDate.now();
        return schoolYearRepository.findAll().stream()
                .filter(year -> year !=null &&  !today.isBefore(year.getStartDate()) && !today.isAfter(year.getEndDate()))
                .findFirst()
                .orElse(null); // إذا لم يجد سنة تغطي التاريخ الحالي
    }
}