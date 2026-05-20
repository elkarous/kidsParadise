package com.kids.repositories;

import com.kids.entities.Student;
import com.kids.entities.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findBySchoolClassId(Long classId);
}
