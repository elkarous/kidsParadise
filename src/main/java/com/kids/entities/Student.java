package com.kids.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String parentContact;
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Parent parent; // Every student has one parent

    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass; // Linked to a specific class
}
