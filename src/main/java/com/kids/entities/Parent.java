package com.kids.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data

public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fatherName;
    private String motherName;
    private String phoneNumber;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER) // شحن البيانات فوراً مع الأب
    private List<Student> children = new ArrayList<>();
    @Override
    public String toString() {
        return fatherName + " " + phoneNumber ;
    }
}
