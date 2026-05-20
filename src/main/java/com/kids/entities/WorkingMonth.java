package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "working_months")
@Getter
@Setter
public class WorkingMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 🌟 هنا أيضاً لضمان توليد معرف الشهر
    private Long id;

    @Column(name = "month_name", nullable = false)
    private String monthName;

    @Column(name = "month_number", nullable = false)
    private int monthNumber;

    // تحديده بـ @Column لضمان أن JPA ينشئه باسم واضح في الجدول ويحقنه بدقة
    @Column(name = "year", nullable = false)
    private int year;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    private boolean closed;
}