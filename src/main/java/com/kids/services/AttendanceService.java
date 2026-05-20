package com.kids.services;

import com.kids.entities.*;
import com.kids.repositories.AttendanceRepository;
import com.kids.repositories.StudentRepository; // افترض وجوده لجلب تلاميذ الفوج
import com.kids.repositories.WorkingMonthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private WorkingMonthRepository workingMonthRepository;
    @Autowired
    private SchoolYearService schoolYearService;

    /**
     * جلب دفتر الحضور لفوج معين في تاريخ محدد.
     * إذا لم يكن هناك سجل سابق في الـ DB، يتم توليد سجلات افتراضية (حاضر) دون حفظها لحين قيام المستخدم بالضغط على حفظ.
     */
// افترض وجوده لجلب الشهر

    @Transactional
    public List<Attendance> getAttendanceRegister(Long classId, LocalDate date) {
        // 1. البحث عن السجلات الموجودة مسبقاً
        List<Attendance> existingAttendance = attendanceRepository.findBySchoolClassIdAndAttendanceDate(classId, date);

        if (existingAttendance != null && !existingAttendance.isEmpty()) {
            return existingAttendance;
        }

        // 2. 🌟 تحديد الشهر العملي بناءً على تاريخ اليوم المختار
        int currentMonth = date.getMonthValue(); // مثلاً: 5
        int currentYear = date.getYear();       // مثلاً: 2026
        SchoolYear schoolYear = schoolYearService.findActualYear();

        WorkingMonth activeMonth = workingMonthRepository.findByMonthValueAndYearValue(currentMonth, currentYear)
                .orElseGet(() -> {
                    // إذا لم يكن الشهر منشأً في النظام مسبقاً، نقوم بإنشائه تلقائياً كأمان
                    WorkingMonth newMonth = new WorkingMonth();
                    newMonth.setMonthName(date.getMonth().name() + " " + currentYear);
                    newMonth.setMonthNumber(currentMonth);
                    newMonth.setSchoolYear(schoolYear);
                    return workingMonthRepository.save(newMonth);
                });

        // 3. جلب التلاميذ وبناء قائمة الحضور الافتراضية وربطها بالشهر
        List<Student> classStudents = studentRepository.findBySchoolClassId(classId);
        List<Attendance> defaultRegister = new ArrayList<>();

        for (Student student : classStudents) {
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setSchoolClass(student.getSchoolClass());
            attendance.setAttendanceDate(date);
            attendance.setWorkingMonth(activeMonth); // 🌟 ربط السجل بالشهر العملي النشط هنا
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setNotes("");
            defaultRegister.add(attendance);
        }
        return defaultRegister;
    }

    /**
     * حفظ أو تحديث دفتر الحضور بالكامل دفعة واحدة
     */
    @Transactional
    public void saveAllAttendance(List<Attendance> attendanceList) {
        attendanceRepository.saveAll(attendanceList);
    }
}