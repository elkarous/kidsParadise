package com.kindererp.service;

import com.kindererp.model.Employee;
import com.kindererp.model.StaffMember;
import com.kindererp.model.StaffStatus;
import com.kindererp.model.Teacher;
import com.kindererp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.kindererp.service.Checks.*;

/** Teachers and administrative employees. */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final TeacherRepository teacherRepository;
    private final EmployeeRepository employeeRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;
    private final TeacherPaymentRepository teacherPaymentRepository;
    private final EmployeePaymentRepository employeePaymentRepository;

    public StaffMember newMember(StaffKind kind) {
        return kind == StaffKind.TEACHER ? new Teacher() : new Employee();
    }

    @Transactional(readOnly = true)
    public List<StaffMember> findAll(StaffKind kind) {
        return new ArrayList<>(kind == StaffKind.TEACHER
                ? teacherRepository.findAllByOrderByNameAsc()
                : employeeRepository.findAllByOrderByNameAsc());
    }

    @Transactional(readOnly = true)
    public List<StaffMember> findActive(StaffKind kind) {
        return new ArrayList<>(kind == StaffKind.TEACHER
                ? teacherRepository.findByStatusOrderByNameAsc(StaffStatus.ACTIVE)
                : employeeRepository.findByStatusOrderByNameAsc(StaffStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<Teacher> teachers() {
        return teacherRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public StaffMember save(StaffMember member) {
        require(!isBlank(member.getName()), "validation.name.required");
        require(member.getHiringDate() != null, "validation.hiringDate.required");
        require(member.getSalaryType() != null, "validation.salaryType.required");
        require(isNonNegative(member.getBaseSalary()), "validation.amount.negative");
        require(isNonNegative(member.getAbsencePenalty()), "validation.amount.negative");
        require(isBlank(member.getPhone()) || isValidPhone(member.getPhone()), "validation.phone.invalid");
        require(isBlank(member.getEmail()) || isValidEmail(member.getEmail()), "validation.email.invalid");

        member.setName(member.getName().trim());
        member.setPosition(trimToNull(member.getPosition()));
        member.setPhone(trimToNull(member.getPhone()));
        member.setEmail(trimToNull(member.getEmail()));
        if (member.getStatus() == null) {
            member.setStatus(StaffStatus.ACTIVE);
        }
        return member instanceof Teacher teacher ? teacherRepository.save(teacher) : employeeRepository.save((Employee) member);
    }

    /**
     * Deletes a staff member with no payroll or attendance history. Members with history must be
     * set to inactive instead, so past salaries stay traceable.
     */
    @Transactional
    public void delete(StaffKind kind, Long id) {
        if (kind == StaffKind.TEACHER) {
            require(!teacherPaymentRepository.existsByTeacherId(id) && !teacherAttendanceRepository.existsByTeacherId(id),
                    "staff.error.hasHistory");
            teacherRepository.deleteById(id);
        } else {
            require(!employeePaymentRepository.existsByEmployeeId(id) && !employeeAttendanceRepository.existsByEmployeeId(id),
                    "staff.error.hasHistory");
            employeeRepository.deleteById(id);
        }
    }
}
