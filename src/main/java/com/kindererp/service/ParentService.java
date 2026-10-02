package com.kindererp.service;

import com.kindererp.model.Parent;
import com.kindererp.model.SchoolSettings;
import com.kindererp.repository.ParentRepository;
import com.kindererp.repository.SchoolSettingsRepository;
import com.kindererp.repository.StudentRepository;
import com.kindererp.repository.TuitionPaymentRepository;
import com.kindererp.service.dto.ParentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kindererp.service.Checks.*;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final SchoolSettingsRepository settingsRepository;
    private final TuitionService tuitionService;

    @Transactional(readOnly = true)
    public List<Parent> findAll() {
        return parentRepository.findAllByOrderByFatherNameAsc();
    }

    /** Every parent with its number of enrolled children and monthly tuition. */
    @Transactional(readOnly = true)
    public List<ParentSummary> findAllSummaries() {
        SchoolSettings settings = settingsRepository.findById(SchoolSettings.SINGLETON_ID).orElseGet(SchoolSettings::new);
        Map<Long, Integer> childCounts = childCountsByParent();
        return parentRepository.findAllByOrderByFatherNameAsc().stream()
                .map(p -> new ParentSummary(p,
                        tuitionService.compute(childCounts.getOrDefault(p.getId(), 0), settings)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> childCountsByParent() {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : studentRepository.countChildrenPerParent()) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    @Transactional
    public Parent save(Parent parent) {
        require(!isBlank(parent.getFatherName()), "validation.fatherName.required");
        require(!isBlank(parent.getPhone()), "validation.phone.required");
        require(isValidPhone(parent.getPhone()), "validation.phone.invalid");
        require(isBlank(parent.getEmail()) || isValidEmail(parent.getEmail()), "validation.email.invalid");
        parent.setFatherName(parent.getFatherName().trim());
        parent.setMotherName(trimToNull(parent.getMotherName()));
        parent.setPhone(parent.getPhone().trim());
        parent.setEmail(trimToNull(parent.getEmail()));
        return parentRepository.save(parent);
    }

    @Transactional
    public void delete(Long parentId) {
        require(studentRepository.countByParentId(parentId) == 0, "parents.error.hasChildren");
        require(!paymentRepository.existsByParentId(parentId), "parents.error.hasPayments");
        parentRepository.deleteById(parentId);
    }
}
