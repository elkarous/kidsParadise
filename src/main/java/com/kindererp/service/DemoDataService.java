package com.kindererp.service;

import com.kindererp.model.*;
import com.kindererp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Optional demo dataset offered by the setup wizard, so a school can try the app before entering
 * its own data. Every name is clearly fictitious ("Exemple" / "تجربة").
 */
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private final LevelRepository levelRepository;
    private final SchoolClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final EmployeeRepository employeeRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void load() {
        Teacher teacherA = teacher("Enseignante Exemple A", "Éveil", "20000001");
        Teacher teacherB = teacher("معلمة تجربة ب", "لغة عربية", "20000002");
        employee("Employé Exemple", "Accueil", "20000003");

        Level small = levelRepository.save(new Level("Petite section (démo)"));
        Level big = levelRepository.save(new Level("Grande section (démo)"));
        SchoolClass classA = schoolClass("Groupe A", small, teacherA);
        SchoolClass classB = schoolClass("Groupe B", big, teacherB);

        Parent family1 = parent("Parent Exemple 1", "Mère Exemple 1", "99000001");
        Parent family2 = parent("وليّ تجربة 2", "أمّ تجربة 2", "99000002");
        Parent family3 = parent("Parent Exemple 3", null, "99000003");

        student("Enfant", "Exemple Un", 2021, family1, classA);
        student("Enfant", "Exemple Deux", 2020, family1, classB);
        student("طفل", "تجربة", 2021, family2, classA);
        student("Enfant", "Exemple Trois", 2020, family3, classB);
    }

    private Teacher teacher(String name, String specialty, String phone) {
        Teacher teacher = new Teacher();
        fillStaff(teacher, name, specialty, phone);
        return teacherRepository.save(teacher);
    }

    private void employee(String name, String jobTitle, String phone) {
        Employee employee = new Employee();
        fillStaff(employee, name, jobTitle, phone);
        employeeRepository.save(employee);
    }

    private static void fillStaff(StaffMember member, String name, String position, String phone) {
        member.setName(name);
        member.setPosition(position);
        member.setPhone(phone);
        member.setHiringDate(LocalDate.now().minusYears(1));
        member.setSalaryType(SalaryType.FIXED_MONTHLY);
        member.setBaseSalary(new BigDecimal("800"));
        member.setAbsencePenalty(new BigDecimal("20"));
        member.setStatus(StaffStatus.ACTIVE);
    }

    private SchoolClass schoolClass(String name, Level level, Teacher teacher) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(name);
        schoolClass.setLevel(level);
        schoolClass.setTeacher(teacher);
        return classRepository.save(schoolClass);
    }

    private Parent parent(String father, String mother, String phone) {
        Parent parent = new Parent();
        parent.setFatherName(father);
        parent.setMotherName(mother);
        parent.setPhone(phone);
        return parentRepository.save(parent);
    }

    private void student(String firstName, String lastName, int birthYear, Parent parent, SchoolClass schoolClass) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setBirthDate(LocalDate.of(birthYear, 3, 15));
        student.setEnrollmentDate(LocalDate.now());
        student.setParent(parent);
        student.setSchoolClass(schoolClass);
        studentRepository.save(student);
    }
}
