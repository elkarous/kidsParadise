package com.kids.services;

import com.kids.entities.Student;
import com.kids.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository repository;

    public List<Student> findAll() { return repository.findAll(); }

    public void save(Student student) { repository.save(student); }

    public void delete(Student student) { repository.delete(student); }
}