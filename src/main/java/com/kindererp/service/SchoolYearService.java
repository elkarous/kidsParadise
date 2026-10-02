package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.model.SchoolYear;
import com.kindererp.model.WorkingMonth;
import com.kindererp.repository.SchoolSettingsRepository;
import com.kindererp.repository.SchoolYearRepository;
import com.kindererp.repository.WorkingMonthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kindererp.service.Checks.require;

@Service
@RequiredArgsConstructor
public class SchoolYearService {

    private static final Pattern YEAR_NAME = Pattern.compile("^(\\d{4})\\s*[-/]\\s*(\\d{4})$");

    private final SchoolYearRepository yearRepository;
    private final WorkingMonthRepository monthRepository;
    private final SchoolSettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public List<SchoolYear> findAll() {
        return yearRepository.findAllByOrderByStartDateDesc();
    }

    /** The year containing today, otherwise the most recent one (null if none exists). */
    @Transactional(readOnly = true)
    public SchoolYear findCurrent() {
        List<SchoolYear> years = findAll();
        LocalDate today = LocalDate.now();
        return years.stream().filter(y -> y.contains(today)).findFirst()
                .orElse(years.isEmpty() ? null : years.get(0));
    }

    @Transactional(readOnly = true)
    public List<WorkingMonth> months(Long schoolYearId) {
        return monthRepository.findBySchoolYearIdOrderByCalendarYearAscCalendarMonthAsc(schoolYearId);
    }

    /** Suggested name for the school year containing today, e.g. "2026-2027". */
    public static String suggestedName(int startMonth) {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= startMonth ? today.getYear() : today.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }

    /**
     * Creates a school year from a name like "2026-2027" and generates its working months,
     * from the start month to the end month configured in the school settings.
     */
    @Transactional
    public SchoolYear createYear(String name) {
        Matcher matcher = YEAR_NAME.matcher(name == null ? "" : name.trim());
        require(matcher.matches(), "schoolYears.error.format");
        int startYear = Integer.parseInt(matcher.group(1));
        int endYear = Integer.parseInt(matcher.group(2));
        require(endYear == startYear + 1, "schoolYears.error.format");
        String normalizedName = startYear + "-" + endYear;
        require(!yearRepository.existsByName(normalizedName), "schoolYears.error.exists", normalizedName);

        SchoolSettings settings = settingsRepository.findById(SchoolSettings.SINGLETON_ID).orElseGet(SchoolSettings::new);
        YearMonth first = YearMonth.of(startYear, settings.getYearStartMonth());
        YearMonth last = settings.getYearEndMonth() >= settings.getYearStartMonth()
                ? YearMonth.of(startYear, settings.getYearEndMonth())
                : YearMonth.of(endYear, settings.getYearEndMonth());

        SchoolYear year = new SchoolYear();
        year.setName(normalizedName);
        year.setStartDate(first.atDay(1));
        year.setEndDate(last.atEndOfMonth());
        SchoolYear saved = yearRepository.save(year);

        for (YearMonth ym = first; !ym.isAfter(last); ym = ym.plusMonths(1)) {
            WorkingMonth month = new WorkingMonth();
            month.setSchoolYear(saved);
            month.setCalendarYear(ym.getYear());
            month.setCalendarMonth(ym.getMonthValue());
            month.setClosed(false);
            monthRepository.save(month);
        }
        return saved;
    }

    @Transactional
    public void setMonthClosed(Long monthId, boolean closed) {
        WorkingMonth month = monthRepository.findById(monthId).orElseThrow(() -> new BusinessException("error.notFound"));
        month.setClosed(closed);
    }
}
