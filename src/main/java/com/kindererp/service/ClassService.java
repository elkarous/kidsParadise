package com.kindererp.service;

import com.kindererp.model.Level;
import com.kindererp.model.SchoolClass;
import com.kindererp.repository.LevelRepository;
import com.kindererp.repository.SchoolClassRepository;
import com.kindererp.repository.StudentAttendanceRepository;
import com.kindererp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.kindererp.service.Checks.*;

/** Levels ("spaces") and the classes inside them. */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final LevelRepository levelRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final StudentAttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public List<Level> levels() {
        return levelRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> classes() {
        return classRepository.findAllByOrderByLevelNameAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> classesOfLevel(Long levelId) {
        return classRepository.findByLevelIdOrderByNameAsc(levelId);
    }

    @Transactional
    public Level saveLevel(Level level) {
        require(!isBlank(level.getName()), "validation.levelName.required");
        level.setName(level.getName().trim());
        return levelRepository.save(level);
    }

    @Transactional
    public void deleteLevel(Long levelId) {
        require(!classRepository.existsByLevelId(levelId), "classes.error.levelHasClasses");
        levelRepository.deleteById(levelId);
    }

    @Transactional
    public SchoolClass saveClass(SchoolClass schoolClass) {
        require(!isBlank(schoolClass.getName()), "validation.className.required");
        require(schoolClass.getLevel() != null, "validation.level.required");
        schoolClass.setName(schoolClass.getName().trim());
        return classRepository.save(schoolClass);
    }

    @Transactional
    public void deleteClass(Long classId) {
        require(!studentRepository.existsBySchoolClassId(classId), "classes.error.classHasStudents");
        require(!attendanceRepository.existsBySchoolClassId(classId), "classes.error.classHasHistory");
        classRepository.deleteById(classId);
    }
}
