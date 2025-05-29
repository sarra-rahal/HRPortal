package com.HR.backend.repository;

import com.HR.backend.model.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String value);
    Optional<Department> findByName(String name);
}