package com.kindererp.service;

import com.kindererp.model.*;
import com.kindererp.repository.ParentRepository;
import com.kindererp.repository.SchoolSettingsRepository;
import com.kindererp.repository.TuitionPaymentRepository;
import com.kindererp.repository.WorkingMonthRepository;
import com.kindererp.service.dto.FeeBreakdown;
import com.kindererp.service.dto.TuitionOverview;
import com.kindererp.service.dto.TuitionRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kindererp.service.Checks.require;
import static com.kindererp.service.Checks.trimToNull;

/** Monthly tuition payments of the families. */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TuitionPaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final WorkingMonthRepository monthRepository;
    private final SchoolSettingsRepository settingsRepository;
    private final ParentService parentService;
    private final TuitionService tuitionService;

    /** Every family with enrolled children (or a payment) for the month, with totals. */
    @Transactional(readOnly = true)
    public TuitionOverview monthOverview(Long workingMonthId) {
        SchoolSettings settings = settings();
        Map<Long, Integer> childCounts = parentService.childCountsByParent();
        Map<Long, TuitionPayment> payments = paymentRepository.findByWorkingMonthId(workingMonthId).stream()
                .collect(Collectors.toMap(p -> p.getParent().getId(), Function.identity()));

        List<TuitionRow> rows = new ArrayList<>();
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        for (Parent parent : parentRepository.findAllByOrderByFatherNameAsc()) {
            TuitionPayment payment = payments.get(parent.getId());
            int childCount = childCounts.getOrDefault(parent.getId(), 0);
            if (payment == null && childCount == 0) {
                continue;
            }
            FeeBreakdown fee = payment != null
                    ? new FeeBreakdown(payment.getChildrenCount(), payment.getAmount().add(payment.getDiscount()),
                    payment.getDiscount(), payment.getAmount())
                    : tuitionService.compute(childCount, settings);
            rows.add(new TuitionRow(parent, fee, payment));
            expected = expected.add(fee.total());
            if (payment != null) {
                collected = collected.add(payment.getAmount());
            }
        }
        return new TuitionOverview(rows, expected, collected);
    }

    /** What the family owes for one month right now. */
    @Transactional(readOnly = true)
    public FeeBreakdown feeFor(Long parentId) {
        int childCount = parentService.childCountsByParent().getOrDefault(parentId, 0);
        return tuitionService.compute(childCount, settings());
    }

    @Transactional
    public TuitionPayment registerPayment(Long parentId, Long workingMonthId, PaymentMethod method, String notes) {
        WorkingMonth month = monthRepository.findById(workingMonthId).orElseThrow(() -> new BusinessException("error.notFound"));
        Parent parent = parentRepository.findById(parentId).orElseThrow(() -> new BusinessException("error.notFound"));
        require(!month.isClosed(), "payments.error.monthClosed");
        require(!paymentRepository.existsByParentIdAndWorkingMonthId(parentId, workingMonthId), "payments.error.alreadyPaid");

        FeeBreakdown fee = feeFor(parentId);
        require(fee.childCount() > 0, "payments.error.noChildren");
        require(fee.total().signum() > 0, "payments.error.noFee");

        TuitionPayment payment = new TuitionPayment();
        payment.setParent(parent);
        payment.setWorkingMonth(month);
        payment.setChildrenCount(fee.childCount());
        payment.setAmount(fee.total());
        payment.setDiscount(fee.discount());
        payment.setPaymentDate(LocalDate.now());
        payment.setMethod(method == null ? PaymentMethod.CASH : method);
        payment.setNotes(trimToNull(notes));
        return paymentRepository.save(payment);
    }

    private SchoolSettings settings() {
        return settingsRepository.findById(SchoolSettings.SINGLETON_ID).orElseGet(SchoolSettings::new);
    }
}
