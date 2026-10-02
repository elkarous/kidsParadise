package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A class / group of children inside a level, with its responsible teacher. */
@Entity
@Table(name = "school_classes")
@Getter
@Setter
@NoArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Override
    public boolean equals(Object o) {
        return o instanceof SchoolClass other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return SchoolClass.class.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
