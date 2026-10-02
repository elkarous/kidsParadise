package com.kindererp.model;

public enum PaymentStatus {
    /** The month is fully paid after this payment. */
    PAID,
    /** Partial payment (advance); something is still due for the month. */
    ADVANCE
}
