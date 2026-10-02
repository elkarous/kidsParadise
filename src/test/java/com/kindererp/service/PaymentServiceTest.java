package com.kindererp.service;

import com.kindererp.IntegrationTest;
import com.kindererp.model.*;
import com.kindererp.service.dto.TuitionOverview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    private WorkingMonth september;
    private Parent family;

    @BeforeEach
    void setUp() {
        settings();
        SchoolYear year = year();
        september = schoolYearService.months(year.getId()).get(0);
        SchoolClass schoolClass = schoolClass("A");
        family = parent("Famille 1");
        student("Ali", family, schoolClass);
        student("Sara", family, schoolClass);
    }

    @Test
    void registersThePaymentWithTheSiblingDiscount() {
        TuitionPayment payment = paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CHEQUE, " n°123 ");

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getChildrenCount()).isEqualTo(2);
        assertThat(payment.getAmount()).isEqualByComparingTo("195");
        assertThat(payment.getDiscount()).isEqualByComparingTo("5");
        assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.now());
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CHEQUE);
        assertThat(payment.getNotes()).isEqualTo("n°123");
    }

    @Test
    void refusesToPayTheSameMonthTwice() {
        paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CASH, null);
        assertThatThrownBy(() -> paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("payments.error.alreadyPaid");
    }

    @Test
    void refusesPaymentsOnAClosedMonth() {
        schoolYearService.setMonthClosed(september.getId(), true);
        assertThatThrownBy(() -> paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CASH, null))
                .hasMessage("payments.error.monthClosed");
    }

    @Test
    void refusesFamiliesWithoutChildren() {
        Parent empty = parent("Sans enfant");
        assertThatThrownBy(() -> paymentService.registerPayment(empty.getId(), september.getId(), PaymentMethod.CASH, null))
                .hasMessage("payments.error.noChildren");
    }

    @Test
    void monthOverviewComputesExpectedCollectedAndPending() {
        Parent other = parent("Famille 2");
        student("Omar", other, schoolClass("B"));
        paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CASH, null);

        TuitionOverview overview = paymentService.monthOverview(september.getId());

        assertThat(overview.rows()).hasSize(2);
        assertThat(overview.expected()).isEqualByComparingTo("295");
        assertThat(overview.collected()).isEqualByComparingTo("195");
        assertThat(overview.pending()).isEqualByComparingTo("100");
    }

    @Test
    void paidAmountStaysAsPaidWhenTheFeeChangesLater() {
        paymentService.registerPayment(family.getId(), september.getId(), PaymentMethod.CASH, null);
        SchoolSettings settings = settingsService.get();
        settings.setMonthlyFee(new java.math.BigDecimal("150"));
        settingsService.save(settings);

        TuitionOverview overview = paymentService.monthOverview(september.getId());
        assertThat(overview.collected()).isEqualByComparingTo("195");
    }
}
