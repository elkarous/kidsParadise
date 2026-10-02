package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A "space" / level of the school (e.g. "Petite section"), grouping several classes. */
@Entity
@Table(name = "levels")
@Getter
@Setter
@NoArgsConstructor
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    public Level(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Level other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Level.class.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
