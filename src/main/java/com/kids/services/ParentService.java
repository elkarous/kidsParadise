package com.kids.services;


import com.kids.entities.Parent;
import com.kids.repositories.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;

    @Transactional(readOnly = true)
    public List<Parent> findAll() {
        return parentRepository.findAll();
    }

    @Transactional
    public Parent save(Parent parent) {
        return parentRepository.save(parent);
    }

    @Transactional
    public void delete(Long id) {
        parentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Parent findById(Long id) {
        return parentRepository.findById(id).orElse(null);
    }
}
