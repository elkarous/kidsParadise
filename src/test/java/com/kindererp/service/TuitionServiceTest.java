package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.dto.FeeBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TuitionServiceTest {

    private final TuitionService service = new TuitionService();

    private static SchoolSettings settings(String fee, String discountTwo, String discountThree) {
        SchoolSettings settings = new SchoolSettings();
        settings.setMonthlyFee(new BigDecimal(fee));
        settings.setDiscountTwoChildren(new BigDecimal(discountTwo));
        settings.setDiscountThreePlusChildren(new BigDecimal(discountThree));
        return settings;
    }

    @Test
    void oneChildPaysTheFullFee() {
        FeeBreakdown fee = service.compute(1, settings("100", "5", "10"));
        assertThat(fee.total()).isEqualByComparingTo("100");
        assertThat(fee.discount()).isEqualByComparingTo("0");
    }

    @Test
    void twoChildrenGetTheTwoChildrenDiscount() {
        FeeBreakdown fee = service.compute(2, settings("100", "5", "10"));
        assertThat(fee.baseAmount()).isEqualByComparingTo("200");
        assertThat(fee.discount()).isEqualByComparingTo("5");
        assertThat(fee.total()).isEqualByComparingTo("195");
    }

    @Test
    void threeOrMoreChildrenGetTheLargerDiscount() {
        assertThat(service.compute(3, settings("100", "5", "10")).total()).isEqualByComparingTo("290");
        assertThat(service.compute(4, settings("100", "5", "10")).total()).isEqualByComparingTo("390");
    }

    @Test
    void noChildrenMeansNothingToPay() {
        assertThat(service.compute(0, settings("100", "5", "10"))).isEqualTo(FeeBreakdown.none());
    }

    @Test
    void discountNeverExceedsTheBaseAmount() {
        FeeBreakdown fee = service.compute(2, settings("10", "50", "50"));
        assertThat(fee.discount()).isEqualByComparingTo("20");
        assertThat(fee.total()).isEqualByComparingTo("0");
    }

    @Test
    void decimalFeesAreExact() {
        FeeBreakdown fee = service.compute(3, settings("85.250", "0", "0.750"));
        assertThat(fee.total()).isEqualByComparingTo("255.000");
    }
}
