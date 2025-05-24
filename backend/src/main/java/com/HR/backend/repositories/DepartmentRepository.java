// src/main/java/com/HR/backend/repositories/DepartmentRepository.java
package com.HR.backend.repositories;

import com.HR.backend.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String value);
    Optional<Department> findByName(String name);
}