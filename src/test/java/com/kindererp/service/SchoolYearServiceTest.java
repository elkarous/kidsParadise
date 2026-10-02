package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.SchoolSettings;
import com.kindererp.model.SchoolYear;
import com.kindererp.model.WorkingMonth;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchoolYearServiceTest extends IntegrationTest {

    @Test
    void createsTheYearWithOneWorkingMonthFromSeptemberToJune() {
        settings();
        SchoolYear year = schoolYearService.createYear("2025 / 2026");

        assertThat(year.getName()).isEqualTo("2025-2026");
        assertThat(year.getStartDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(year.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        List<WorkingMonth> months = schoolYearService.months(year.getId());
        assertThat(months).hasSize(10);
        assertThat(months.get(0).yearMonth()).hasToString("2025-09");
        assertThat(months.get(9).yearMonth()).hasToString("2026-06");
    }

    @Test
    void followsTheCalendarConfiguredForTheSchool() {
        SchoolSettings settings = settings();
        settings.setYearStartMonth(10);
        settings.setYearEndMonth(7);
        settingsService.save(settings);

        SchoolYear year = schoolYearService.createYear("2025-2026");
        assertThat(schoolYearService.months(year.getId())).hasSize(10);
        assertThat(year.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void rejectsBadNamesAndDuplicates() {
        settings();
        assertThatThrownBy(() -> schoolYearService.createYear("2025")).hasMessage("schoolYears.error.format");
        assertThatThrownBy(() -> schoolYearService.createYear("2025-2027")).hasMessage("schoolYears.error.format");
        schoolYearService.createYear("2025-2026");
        assertThatThrownBy(() -> schoolYearService.createYear("2025-2026")).hasMessage("schoolYears.error.exists");
    }
}
