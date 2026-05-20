package com.kids.services;

import com.kids.entities.SchoolClass;
import com.kids.repositories.SchoolClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolClassService {
    @Autowired
    private SchoolClassRepository classRepository;


    public List<SchoolClass> findAll() { return classRepository.findAll(); }
    public List<SchoolClass> findByLevelId(Long levelId) { return classRepository.findByLevelId(levelId); }
    public SchoolClass save(SchoolClass schoolClass) { return classRepository.save(schoolClass); }
    public void deleteById(Long id) { classRepository.deleteById(id); }
}
