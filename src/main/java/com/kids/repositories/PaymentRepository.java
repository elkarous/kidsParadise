package com.kids.repositories;

import com.kids.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // جلب كل المدفوعات الخاصة بولي أمر معين
    List<Payment> findByParentId(Long parentId);

    // التحقق مما إذا كان الولي قد دفع شهريّة معينة مسبقاً (لمنع التكرار)
    boolean existsByParentIdAndTargetMonth(Long parentId, String targetMonth);
}