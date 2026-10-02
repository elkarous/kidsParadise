package com.kindererp.service.dto;

import java.math.BigDecimal;
import java.util.List;

/** Tuition status of every family for one month, with totals. */
public record TuitionOverview(List<TuitionRow> rows, BigDecimal expected, BigDecimal collected) {

    public BigDecimal pending() {
        return expected.subtract(collected).max(BigDecimal.ZERO);
    }
}
