package com.kids.repositories;

import com.kids.entities.WorkingMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface WorkingMonthRepository extends JpaRepository<WorkingMonth, Long> {
    @Query("SELECT w FROM WorkingMonth w WHERE w.monthNumber = :currentMonth " +
            "AND w.schoolYear.yearName LIKE CONCAT('%', :currentYear, '%')")
    Optional<WorkingMonth> findByMonthValueAndYearValue(
            @Param("currentMonth") int currentMonth,
            @Param("currentYear") int currentYear
    );
}
