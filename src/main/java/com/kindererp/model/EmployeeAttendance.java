package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_attendance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
public class EmployeeAttendance extends StaffAttendanceRecord {

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
}
