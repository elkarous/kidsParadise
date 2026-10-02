package com.kindererp.service;

import com.kindererp.model.AttendanceStatus;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Student;
import com.kindererp.model.StudentAttendance;
import com.kindererp.repository.SchoolClassRepository;
import com.kindererp.repository.StudentAttendanceRepository;
import com.kindererp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kindererp.service.Checks.require;

/** Daily attendance register of the children, per class. */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final StudentAttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;

    /**
     * The register of a class for a day: saved lines where they exist, otherwise a new "present"
     * line for each child of the class (not saved until {@link #saveRegister} is called).
     */
    @Transactional(readOnly = true)
    public List<StudentAttendance> getRegister(Long classId, LocalDate date) {
        SchoolClass schoolClass = classRepository.findById(classId).orElseThrow(() -> new BusinessException("error.notFound"));
        Map<Long, StudentAttendance> saved = attendanceRepository.findBySchoolClassIdAndAttendanceDate(classId, date).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));

        List<StudentAttendance> register = new ArrayList<>();
        for (Student student : studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId)) {
            StudentAttendance line = saved.remove(student.getId());
            if (line == null) {
                line = new StudentAttendance();
                line.setStudent(student);
                line.setSchoolClass(schoolClass);
                line.setAttendanceDate(date);
                line.setStatus(AttendanceStatus.PRESENT);
            }
            register.add(line);
        }
        // Children who left the class since that day keep their saved line.
        register.addAll(saved.values());
        return register;
    }

    @Transactional
    public void saveRegister(List<StudentAttendance> register) {
        for (StudentAttendance line : register) {
            require(!line.getAttendanceDate().isAfter(LocalDate.now()), "attendance.error.futureDate");
        }
        attendanceRepository.saveAll(register);
    }

    @Transactional(readOnly = true)
    public long countAbsences(Long studentId, LocalDate from, LocalDate to) {
        return attendanceRepository.countByStudentIdAndStatusAndAttendanceDateBetween(studentId, AttendanceStatus.ABSENT, from, to);
    }
}
