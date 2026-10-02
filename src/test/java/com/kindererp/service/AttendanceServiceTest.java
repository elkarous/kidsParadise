package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.AttendanceStatus;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Student;
import com.kindererp.model.StudentAttendance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceServiceTest extends IntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2025, 10, 6);

    @Autowired
    private AttendanceService attendanceService;

    private SchoolClass classA;
    private Student ali;
    private Student sara;

    @BeforeEach
    void setUp() {
        classA = schoolClass("A");
        var parent = parent("Parent");
        ali = student("Ali", parent, classA);
        sara = student("Sara", parent, classA);
        student("Omar", parent, schoolClass("B"));
    }

    @Test
    void newRegisterListsEveryChildOfTheClassAsPresent() {
        List<StudentAttendance> register = attendanceService.getRegister(classA.getId(), DAY);

        assertThat(register).extracting(a -> a.getStudent().getFirstName()).containsExactlyInAnyOrder("Ali", "Sara");
        assertThat(register).allMatch(a -> a.getStatus() == AttendanceStatus.PRESENT && a.getId() == null);
    }

    @Test
    void savedRegisterIsReloadedAndCanBeUpdated() {
        List<StudentAttendance> register = attendanceService.getRegister(classA.getId(), DAY);
        register.stream().filter(a -> a.getStudent().equals(ali)).findFirst().orElseThrow().setStatus(AttendanceStatus.ABSENT);
        attendanceService.saveRegister(register);

        List<StudentAttendance> reloaded = attendanceService.getRegister(classA.getId(), DAY);
        assertThat(reloaded).allMatch(a -> a.getId() != null);
        assertThat(reloaded).filteredOn(a -> a.getStudent().equals(ali)).extracting(StudentAttendance::getStatus)
                .containsExactly(AttendanceStatus.ABSENT);

        reloaded.forEach(a -> a.setStatus(AttendanceStatus.PRESENT));
        attendanceService.saveRegister(reloaded);
        assertThat(attendanceService.getRegister(classA.getId(), DAY)).hasSize(2)
                .allMatch(a -> a.getStatus() == AttendanceStatus.PRESENT);
    }

    @Test
    void countsAbsencesOverAPeriod() {
        for (LocalDate day : List.of(DAY, DAY.plusDays(1), DAY.plusDays(2))) {
            List<StudentAttendance> register = attendanceService.getRegister(classA.getId(), day);
            register.stream().filter(a -> a.getStudent().equals(sara)).forEach(a -> a.setStatus(AttendanceStatus.ABSENT));
            attendanceService.saveRegister(register);
        }
        assertThat(attendanceService.countAbsences(sara.getId(), DAY, DAY.plusDays(1))).isEqualTo(2);
        assertThat(attendanceService.countAbsences(ali.getId(), DAY, DAY.plusDays(2))).isZero();
    }

    @Test
    void refusesAttendanceForAFutureDate() {
        List<StudentAttendance> register = attendanceService.getRegister(classA.getId(), LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> attendanceService.saveRegister(register)).hasMessage("attendance.error.futureDate");
    }
}
