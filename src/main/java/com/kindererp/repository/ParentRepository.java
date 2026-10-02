package com.kindererp.repository;

import com.kindererp.model.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    List<Parent> findAllByOrderByFatherNameAsc();
}
