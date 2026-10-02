package com.kindererp.repository;

import com.kindererp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByOrderByLastNameAscFirstNameAsc();

    List<Student> findBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long classId);

    long countByParentId(Long parentId);

    boolean existsBySchoolClassId(Long classId);

    /** Number of enrolled children per parent, as [parentId, count] rows. */
    @Query("select s.parent.id, count(s) from Student s where s.parent is not null group by s.parent.id")
    List<Object[]> countChildrenPerParent();
}
