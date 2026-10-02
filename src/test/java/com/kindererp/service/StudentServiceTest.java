package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentServiceTest extends IntegrationTest {

    @Autowired
    private StudentService studentService;

    private Student newStudent(SchoolClass schoolClass) {
        Student student = new Student();
        student.setFirstName("  Yasmine ");
        student.setLastName("Exemple");
        student.setBirthDate(LocalDate.now().minusYears(4));
        student.setSchoolClass(schoolClass);
        student.setParent(parent("Parent"));
        return student;
    }

    @Test
    void registersAStudentWithTrimmedNamesAndTodayAsEnrollmentDate() {
        Student saved = studentService.save(newStudent(schoolClass("A")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Yasmine");
        assertThat(saved.getEnrollmentDate()).isEqualTo(LocalDate.now());
        assertThat(studentService.findAll()).extracting(Student::getId).contains(saved.getId());
    }

    @Test
    void rejectsAMissingFirstName() {
        Student student = newStudent(schoolClass("A"));
        student.setFirstName(" ");
        assertThatThrownBy(() -> studentService.save(student))
                .isInstanceOf(BusinessException.class)
                .hasMessage("validation.firstName.required");
    }

    @Test
    void rejectsABirthDateInTheFuture() {
        Student student = newStudent(schoolClass("A"));
        student.setBirthDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> studentService.save(student)).hasMessage("validation.birthDate.future");
    }

    @Test
    void requiresAClass() {
        Student student = newStudent(null);
        assertThatThrownBy(() -> studentService.save(student)).hasMessage("validation.class.required");
    }

    @Test
    void findsStudentsByClass() {
        SchoolClass a = schoolClass("A");
        SchoolClass b = schoolClass("B");
        studentService.save(newStudent(a));
        studentService.save(newStudent(b));
        assertThat(studentService.findByClass(a.getId())).hasSize(1);
    }
}
