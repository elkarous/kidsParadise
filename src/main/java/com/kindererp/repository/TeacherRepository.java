package com.kindererp.repository;

import com.kindererp.model.StaffStatus;
import com.kindererp.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    List<Teacher> findAllByOrderByNameAsc();

    List<Teacher> findByStatusOrderByNameAsc(StaffStatus status);
}
