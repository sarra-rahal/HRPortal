// src/main/java/com/HR/backend/services/EmployeeService.java
package com.HR.backend.services;

import com.HR.backend.models.Department;
import com.HR.backend.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final DepartmentRepository departmentRepository;

    public List<String> getDepartments() {
        return departmentRepository.findAll().stream().map(Department::getName).toList();
    }

    public List<Map<String, String>> getEmployees(String department) {
        // Mock data (replace with EmployeeRepository)
        List<Map<String, String>> employees = List.of(
                Map.of("cin", "123", "name", "John Doe"),
                Map.of("cin", "456", "name", "Jane Smith")
        );
        return employees;
    }

    public String getUserDepartment(String email) {
        return "IT"; // Query Employee table
    }

    public String getUserCin(String email) {
        return "123"; // Query Employee table
    }
}