package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.dto.FeeBreakdown;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Monthly tuition rule: fee per child, minus a family discount for siblings (amounts from school settings). */
@Service
public class TuitionService {

    public FeeBreakdown compute(int childCount, SchoolSettings settings) {
        if (childCount <= 0) {
            return FeeBreakdown.none();
        }
        BigDecimal base = settings.getMonthlyFee().multiply(BigDecimal.valueOf(childCount));
        BigDecimal discount = switch (childCount) {
            case 1 -> BigDecimal.ZERO;
            case 2 -> settings.getDiscountTwoChildren();
            default -> settings.getDiscountThreePlusChildren();
        };
        discount = discount.min(base);
        return new FeeBreakdown(childCount, base, discount, base.subtract(discount));
    }
}
