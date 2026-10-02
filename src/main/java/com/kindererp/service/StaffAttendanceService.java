package com.kindererp.service;

import com.kindererp.model.*;
import com.kindererp.repository.EmployeeAttendanceRepository;
import com.kindererp.repository.EmployeeRepository;
import com.kindererp.repository.TeacherAttendanceRepository;
import com.kindererp.repository.TeacherRepository;
import com.kindererp.service.dto.StaffAttendanceRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kindererp.service.Checks.require;
import static com.kindererp.service.Checks.trimToNull;

/** Daily attendance (status, arrival/departure time, sessions) of teachers and employees. */
@Service
@RequiredArgsConstructor
public class StaffAttendanceService {

    public static final LocalTime DEFAULT_CHECK_IN = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_CHECK_OUT = LocalTime.of(16, 0);

    private final StaffService staffService;
    private final TeacherRepository teacherRepository;
    private final EmployeeRepository employeeRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;

    /** One row per active staff member: the saved record of the day, or a default "present" row. */
    @Transactional(readOnly = true)
    public List<StaffAttendanceRow> day(StaffKind kind, LocalDate date) {
        Map<Long, ? extends StaffAttendanceRecord> saved = savedRecords(kind, date);
        return staffService.findActive(kind).stream().map(member -> {
            StaffAttendanceRecord record = saved.get(member.getId());
            return record == null
                    ? new StaffAttendanceRow(member.getId(), member.getName(), AttendanceStatus.PRESENT,
                    DEFAULT_CHECK_IN, DEFAULT_CHECK_OUT, 1, null)
                    : new StaffAttendanceRow(member.getId(), member.getName(), record.getStatus(),
                    record.getCheckIn(), record.getCheckOut(), record.getSessionsCount(), record.getNotes());
        }).toList();
    }

    @Transactional
    public void saveDay(StaffKind kind, LocalDate date, List<StaffAttendanceRow> rows) {
        require(date != null && !date.isAfter(LocalDate.now()), "attendance.error.futureDate");
        Map<Long, ? extends StaffAttendanceRecord> saved = savedRecords(kind, date);

        for (StaffAttendanceRow row : rows) {
            require(row.getCheckIn() == null || row.getCheckOut() == null || !row.getCheckOut().isBefore(row.getCheckIn()),
                    "attendance.error.timeOrder", row.getName());
            require(row.getSessions() >= 0, "attendance.error.sessions", row.getName());

            StaffAttendanceRecord record = saved.get(row.getStaffId());
            if (record == null) {
                record = newRecord(kind, row.getStaffId());
                record.setAttendanceDate(date);
            }
            record.setStatus(row.getStatus());
            record.setCheckIn(row.getStatus().isPresent() ? row.getCheckIn() : null);
            record.setCheckOut(row.getStatus().isPresent() ? row.getCheckOut() : null);
            record.setSessionsCount(row.getStatus().isPresent() ? row.getSessions() : 0);
            record.setNotes(trimToNull(row.getNotes()));
            if (record instanceof TeacherAttendance teacherRecord) {
                teacherAttendanceRepository.save(teacherRecord);
            } else {
                employeeAttendanceRepository.save((EmployeeAttendance) record);
            }
        }
    }

    /** Attendance records of one staff member over a period (used by payroll). */
    @Transactional(readOnly = true)
    public List<? extends StaffAttendanceRecord> records(StaffKind kind, Long staffId, LocalDate from, LocalDate to) {
        return kind == StaffKind.TEACHER
                ? teacherAttendanceRepository.findByTeacherIdAndAttendanceDateBetween(staffId, from, to)
                : employeeAttendanceRepository.findByEmployeeIdAndAttendanceDateBetween(staffId, from, to);
    }

    private Map<Long, ? extends StaffAttendanceRecord> savedRecords(StaffKind kind, LocalDate date) {
        if (kind == StaffKind.TEACHER) {
            return teacherAttendanceRepository.findByAttendanceDate(date).stream()
                    .collect(Collectors.toMap(a -> a.getTeacher().getId(), Function.identity()));
        }
        return employeeAttendanceRepository.findByAttendanceDate(date).stream()
                .collect(Collectors.toMap(a -> a.getEmployee().getId(), Function.identity()));
    }

    private StaffAttendanceRecord newRecord(StaffKind kind, Long staffId) {
        if (kind == StaffKind.TEACHER) {
            TeacherAttendance record = new TeacherAttendance();
            record.setTeacher(teacherRepository.getReferenceById(staffId));
            return record;
        }
        EmployeeAttendance record = new EmployeeAttendance();
        record.setEmployee(employeeRepository.getReferenceById(staffId));
        return record;
    }
}
