package com.kids.repositories;

import com.kids.entities.Attendance;
import com.kids.entities.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySchoolClassIdAndAttendanceDate(Long classId, LocalDate date);
    // جلب كل سجلات الحضور لفوج معين في تاريخ معين
    // 🌟 الاستعلام السحري الجديد: جلب غيابات تلميذ محدد خلال شهر عملي محدد بالكامل!
    List<Attendance> findByStudentIdAndWorkingMonthIdAndStatus(Long studentId, Long workingMonthId, AttendanceStatus status);
}
