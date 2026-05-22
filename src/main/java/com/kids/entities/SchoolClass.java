package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString
public class SchoolClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String className; // e.g., "Group A"

    @ManyToOne
    @JoinColumn(name = "level_id")
    @ToString.Exclude // This prevents the infinite loop
    private Level level;

    @ManyToOne(fetch = FetchType.EAGER ) // 🌟 تأكد أنها ManyToOne وليست OneToMany
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

}