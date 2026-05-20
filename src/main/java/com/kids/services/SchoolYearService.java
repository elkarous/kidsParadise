package com.kids.services;

import com.kids.entities.SchoolYear;
import com.kids.entities.WorkingMonth;
import com.kids.repositories.SchoolYearRepository;
import com.kids.repositories.WorkingMonthRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SchoolYearService {

    @Autowired
    private SchoolYearRepository schoolYearRepository;
    @Autowired private WorkingMonthRepository monthRepository;

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


    public SchoolYear createSchoolYearWithMonths(String yearName) {
        // 1. تفكيك نص السنة الدراسية
        String[] parts = yearName.split("-");
        int startYear = Integer.parseInt(parts[0].trim());
        int endYear = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : startYear + 1;

        // 2. بناء وحفظ السنة الدراسية
        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setYearName(yearName);
        schoolYear.setStartDate(LocalDate.of(startYear, 10, 1));
        schoolYear.setEndDate(LocalDate.of(endYear, 6, 30));

        // حفظ كائن الأب
        SchoolYear savedYear = schoolYearRepository.save(schoolYear);

        // 3. توليد وحفظ الأشهر باستخدام الحقول الصحيحة للكلاس (monthNumber و year)

        // الجزء الأول: أشهر سنة البداية (من سبتمبر إلى ديسمبر)
        String[] firstPartMonths = {"سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
        int monthVal = 9;
        for (String mName : firstPartMonths) {
            saveSingleMonth(mName + " " + startYear, monthVal++, startYear, savedYear);
        }

        // الجزء الثاني: أشهر سنة النهاية (من جانفي إلى جوان)
        String[] secondPartMonths = {"جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان"};
        monthVal = 1;
        for (String mName : secondPartMonths) {
            saveSingleMonth(mName + " " + endYear, monthVal++, endYear, savedYear);
        }

        return savedYear;
    }

    private void saveSingleMonth(String name, int num, int yr, SchoolYear schoolYear) {
        WorkingMonth wm = new WorkingMonth();
        wm.setMonthName(name);
        wm.setMonthNumber(num);  // 🌟 تعديل: استخدام الاسم الصحيح المطابق للـ Entity
        wm.setYear(yr);          // 🌟 تعديل: استخدام الاسم الصحيح المطابق للـ Entity
        wm.setSchoolYear(schoolYear);
        wm.setClosed(false);

        // حفظ مباشر في قاعدة البيانات
        wm = monthRepository.save(wm);
        System.out.println(wm.getId());
    }

    public List<SchoolYear> findAll() {
        return schoolYearRepository.findAll();
    }
    }
