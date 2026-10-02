package com.kindererp.repository;

import com.kindererp.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findAllByOrderByLevelNameAscNameAsc();

    List<SchoolClass> findByLevelIdOrderByNameAsc(Long levelId);

    boolean existsByLevelId(Long levelId);
}
