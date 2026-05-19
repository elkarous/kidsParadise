package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "working_months")
@Data
public class WorkingMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String monthName; // مثال: "أكتوبر 2025"
    private int monthNumber;  // 10

    // العديد من الأشهر تتبع لسنة دراسية واحدة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    // الشهر الواحد فيه العديد من الحصص
    @OneToMany(mappedBy = "workingMonth", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchoolSession> sessions = new ArrayList<>();

    // Getters, Setters, Constructors
}
