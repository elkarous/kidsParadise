package com.kindererp.repository;

import com.kindererp.model.TeacherPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherPaymentRepository extends JpaRepository<TeacherPayment, Long> {

    List<TeacherPayment> findByTeacherIdAndWorkingMonthId(Long teacherId, Long workingMonthId);

    boolean existsByTeacherId(Long teacherId);
}
