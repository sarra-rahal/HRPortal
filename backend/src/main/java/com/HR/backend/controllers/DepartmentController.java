// src/main/java/com/HR/backend/controllers/DepartmentController.java
package com.HR.backend.controllers;

import com.HR.backend.models.Department;
import com.HR.backend.models.Employee;
import com.HR.backend.repositories.DepartmentRepository;
import com.HR.backend.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/departments")
    public ResponseEntity<List<String>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll().stream().map(Department::getName).toList());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, String>>> getEmployees(@RequestParam(required = false) String department) {
        List<Employee> employees = department != null && !department.isEmpty()
                ? employeeRepository.findByDepartement(department)
                : employeeRepository.findAll();
        List<Map<String, String>> result = employees.stream()
                .map(emp -> Map.of(
                        "cin", emp.getCin(),
                        "name", emp.getFirstname() + " " + emp.getLastname()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}