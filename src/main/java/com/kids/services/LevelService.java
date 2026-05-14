package com.kids.services;

import com.kids.entities.Level;
import com.kids.repositories.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final LevelRepository levelRepository;

    @Transactional(readOnly = true)
    public List<Level> findAll() {
        return levelRepository.findAll();
    }
    @Transactional(readOnly = true)
    public List<Level> findAllWithClasses() {
        List<Level> levels = levelRepository.findAll();
        // Manually trigger the loading of the lazy collection
        levels.forEach(level -> level.getClasses().size());
        return levels;
    }
    @Transactional
    public Level save(Level level) {
        return levelRepository.save(level);
    }

    @Transactional
    public void delete(Long id) {
        levelRepository.deleteById(id);
    }

    public long count() {
        return levelRepository.count();
    }
}
