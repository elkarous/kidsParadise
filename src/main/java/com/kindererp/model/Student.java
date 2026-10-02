package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    public String fullName() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Student other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Student.class.hashCode();
    }

    @Override
    public String toString() {
        return fullName();
    }
}
