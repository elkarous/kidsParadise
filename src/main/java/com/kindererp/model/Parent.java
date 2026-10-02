package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A family (father/mother) responsible for one or more children and for paying tuition. */
@Entity
@Table(name = "parents")
@Getter
@Setter
@NoArgsConstructor
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(name = "father_name", nullable = false, length = 150)
    private String fatherName;

    @Column(name = "mother_name", length = 150)
    private String motherName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Override
    public boolean equals(Object o) {
        return o instanceof Parent other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Parent.class.hashCode();
    }

    @Override
    public String toString() {
        return fatherName + " (" + phone + ")";
    }
}
