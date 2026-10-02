package com.kindererp.service.dto;

import com.kindererp.model.Parent;
import com.kindererp.model.TuitionPayment;

/** One family in the monthly tuition screen. {@code payment} is null when the month is unpaid. */
public record TuitionRow(Parent parent, FeeBreakdown fee, TuitionPayment payment) {

    public boolean paid() {
        return payment != null;
    }
}
