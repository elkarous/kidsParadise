package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A person allowed to log into the application. Passwords are stored as BCrypt hashes only. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Override
    public String toString() {
        return username;
    }
}
