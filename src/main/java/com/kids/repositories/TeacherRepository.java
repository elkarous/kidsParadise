package com.kids.repositories;

// ─── TeacherRepository ───────────────────────────────────────────────────────

import com.kids.entities.Status;
import com.kids.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    List<Teacher> findByStatus(Status status);

    @Query("SELECT t FROM Teacher t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Teacher> searchByName(@Param("q") String query);
}

