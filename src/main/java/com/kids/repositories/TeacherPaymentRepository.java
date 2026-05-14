package com.kids.repositories;

import com.kids.entities.TeacherPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherPaymentRepository extends JpaRepository<TeacherPayment, Long> {

    @Query("SELECT p.netAmount FROM TeacherPayment p WHERE p.teacher.id = :teacherId")
    Optional<BigDecimal>  sumPaymentsForMonth(String string);
    @Query("SELECT p.netAmount FROM TeacherPayment p WHERE p.teacher.id = :teacherId AND p.paymentStatus = 'PENDING'")
    Optional<BigDecimal> sumOutstandingForMonth(String string);
}
