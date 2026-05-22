package com.kids.repositories;

import com.kids.entities.SchoolClass;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findByLevelId(Long id);
    @Modifying
    @Transactional
    @Query("DELETE FROM SchoolClass c WHERE c.id = :id")
    void forceDeleteById(@Param("id") Long id);
}
