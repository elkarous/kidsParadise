package com.kindererp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@NoArgsConstructor
public class Teacher extends StaffMember {

    @Column(length = 100)
    private String specialty;

    @Override
    public String getPosition() {
        return specialty;
    }

    @Override
    public void setPosition(String position) {
        this.specialty = position;
    }
}
