package com.HR.backend.repositories;

import com.HR.backend.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByCin(String cin);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDepartmentId(Long departmentId);
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByCinContaining(String cin);
    boolean existsByJobtitle(String jobTitle);
    List<Employee> findByJobtitle(String jobTitle);
}