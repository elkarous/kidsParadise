package com.kindererp.repository;

import com.kindererp.model.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {

    List<SchoolYear> findAllByOrderByStartDateDesc();

    boolean existsByName(String name);
}
