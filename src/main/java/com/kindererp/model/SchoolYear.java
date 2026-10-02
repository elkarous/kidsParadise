package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "school_years")
@Getter
@Setter
@NoArgsConstructor
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    /** e.g. "2026-2027" */
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SchoolYear other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return SchoolYear.class.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
