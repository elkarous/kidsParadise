package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_payments")
@Getter
@Setter
@NoArgsConstructor
public class TeacherPayment extends StaffPaymentRecord {

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
}
