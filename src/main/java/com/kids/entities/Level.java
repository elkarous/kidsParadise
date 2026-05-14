package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@ToString
public class Level {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String levelName; // e.g., "Prep", "Level 1"

    @OneToMany(mappedBy = "level", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<SchoolClass> classes;
}

