package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.YearMonth;

/** A billable month of a school year. Tuition and salaries are tracked per working month. */
@Entity
@Table(name = "working_months")
@Getter
@Setter
@NoArgsConstructor
public class WorkingMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear;

    @Column(name = "calendar_month", nullable = false)
    private int calendarMonth;

    /** A closed month accepts no more payments. */
    @Column(nullable = false)
    private boolean closed;

    public YearMonth yearMonth() {
        return YearMonth.of(calendarYear, calendarMonth);
    }

    public LocalDate firstDay() {
        return yearMonth().atDay(1);
    }

    public LocalDate lastDay() {
        return yearMonth().atEndOfMonth();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WorkingMonth other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return WorkingMonth.class.hashCode();
    }
}
