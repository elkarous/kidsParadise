package com.kindererp.repository;

import com.kindererp.model.WorkingMonth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingMonthRepository extends JpaRepository<WorkingMonth, Long> {

    List<WorkingMonth> findBySchoolYearIdOrderByCalendarYearAscCalendarMonthAsc(Long schoolYearId);
}
