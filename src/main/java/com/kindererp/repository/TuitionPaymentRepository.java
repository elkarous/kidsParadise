package com.kindererp.repository;

import com.kindererp.model.TuitionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TuitionPaymentRepository extends JpaRepository<TuitionPayment, Long> {

    boolean existsByParentIdAndWorkingMonthId(Long parentId, Long workingMonthId);

    boolean existsByParentId(Long parentId);

    List<TuitionPayment> findByWorkingMonthId(Long workingMonthId);
}
