package com.kindererp.model;

public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED;

    /** Whether the person was physically there (counts as a worked session for staff). */
    public boolean isPresent() {
        return this == PRESENT || this == LATE;
    }
}
