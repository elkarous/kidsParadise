package com.kindererp.service.dto;

import java.math.BigDecimal;

/** Monthly tuition of a family: base (fee x children), sibling discount and amount due. */
public record FeeBreakdown(int childCount, BigDecimal baseAmount, BigDecimal discount, BigDecimal total) {

    public static FeeBreakdown none() {
        return new FeeBreakdown(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
