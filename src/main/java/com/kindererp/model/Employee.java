package com.kindererp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** Administrative or service staff (director, accountant, cleaning, ...). */
@Entity
@Table(name = "employees")
@NoArgsConstructor
public class Employee extends StaffMember {

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Override
    public String getPosition() {
        return jobTitle;
    }

    @Override
    public void setPosition(String position) {
        this.jobTitle = position;
    }
}
