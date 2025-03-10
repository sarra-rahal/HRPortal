package com.HR.backend.repository;

import com.HR.backend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    boolean existsByCin(String cin);
    Optional<Employee> findByCin(String cin);
}