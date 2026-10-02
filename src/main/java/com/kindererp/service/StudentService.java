package com.kindererp.service;

import com.kindererp.model.Student;
import com.kindererp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.kindererp.service.Checks.*;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repository;

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return repository.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Student> findByClass(Long classId) {
        return repository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
    }

    /** Registers a new student or updates an existing one. */
    @Transactional
    public Student save(Student student) {
        require(!isBlank(student.getFirstName()), "validation.firstName.required");
        require(!isBlank(student.getLastName()), "validation.lastName.required");
        require(student.getBirthDate() != null, "validation.birthDate.required");
        require(!student.getBirthDate().isAfter(LocalDate.now()), "validation.birthDate.future");
        require(student.getSchoolClass() != null, "validation.class.required");

        student.setFirstName(student.getFirstName().trim());
        student.setLastName(student.getLastName().trim());
        if (student.getEnrollmentDate() == null) {
            student.setEnrollmentDate(LocalDate.now());
        }
        return repository.save(student);
    }

    /** Deletes the student and its attendance history (cascade in the database). */
    @Transactional
    public void delete(Long studentId) {
        repository.deleteById(studentId);
    }
}
