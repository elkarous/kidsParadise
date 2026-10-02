package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_attendance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
public class TeacherAttendance extends StaffAttendanceRecord {

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
}
