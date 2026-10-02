package com.kindererp.service.dto;

import com.kindererp.model.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

/** Editable attendance line of one staff member for one day. */
@Data
@AllArgsConstructor
public class StaffAttendanceRow {

    private Long staffId;
    private String name;
    private AttendanceStatus status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private int sessions;
    private String notes;
}
