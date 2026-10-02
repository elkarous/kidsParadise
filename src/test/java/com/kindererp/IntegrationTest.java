package com.kindererp;

import com.kindererp.model.*;
import com.kindererp.repository.*;
import com.kindererp.service.SchoolSettingsService;
import com.kindererp.service.SchoolYearService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Base for service tests: a full Spring context on a temporary SQLite file created by the Flyway
 * migrations (never the real database). Each test runs in a transaction that is rolled back.
 */
@SpringBootTest
@Transactional
public abstract class IntegrationTest {

    /** One fresh database file per test run, under target/ (removed by mvn clean). */
    private static final Path TEST_DB = Path.of("target", "test-db", "kindererp-test-" + System.currentTimeMillis() + ".db");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("kindererp.db", TEST_DB::toString);
    }

    @Autowired protected SchoolSettingsService settingsService;
    @Autowired protected SchoolYearService schoolYearService;
    @Autowired protected LevelRepository levelRepository;
    @Autowired protected SchoolClassRepository classRepository;
    @Autowired protected ParentRepository parentRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected EmployeeRepository employeeRepository;

    /** Settings with a monthly fee of 100, discounts of 5 (2 children) and 10 (3+ children). */
    protected SchoolSettings settings() {
        SchoolSettings settings = new SchoolSettings();
        settings.setNameFr("École de test");
        settings.setMonthlyFee(new BigDecimal("100"));
        settings.setDiscountTwoChildren(new BigDecimal("5"));
        settings.setDiscountThreePlusChildren(new BigDecimal("10"));
        settings.setSetupCompleted(true);
        return settingsService.save(settings);
    }

    protected SchoolYear year() {
        return schoolYearService.createYear("2025-2026");
    }

    protected SchoolClass schoolClass(String name) {
        Level level = levelRepository.save(new Level("Niveau " + name));
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(name);
        schoolClass.setLevel(level);
        return classRepository.save(schoolClass);
    }

    protected Parent parent(String name) {
        Parent parent = new Parent();
        parent.setFatherName(name);
        parent.setPhone("20 123 456");
        return parentRepository.save(parent);
    }

    protected Student student(String firstName, Parent parent, SchoolClass schoolClass) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName("Test");
        student.setBirthDate(LocalDate.of(2021, 1, 10));
        student.setEnrollmentDate(LocalDate.of(2025, 9, 1));
        student.setParent(parent);
        student.setSchoolClass(schoolClass);
        return studentRepository.save(student);
    }

    protected Teacher teacher(SalaryType type, String baseSalary, String penalty) {
        Teacher teacher = new Teacher();
        teacher.setName("Enseignant " + type);
        teacher.setHiringDate(LocalDate.of(2024, 9, 1));
        teacher.setSalaryType(type);
        teacher.setBaseSalary(new BigDecimal(baseSalary));
        teacher.setAbsencePenalty(new BigDecimal(penalty));
        teacher.setStatus(StaffStatus.ACTIVE);
        return teacherRepository.save(teacher);
    }
}
