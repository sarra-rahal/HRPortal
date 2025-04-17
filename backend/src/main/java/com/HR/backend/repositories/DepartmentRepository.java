// src/main/java/com/HR/backend/repositories/DepartmentRepository.java
package com.HR.backend.repositories;

import com.HR.backend.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String value);
}