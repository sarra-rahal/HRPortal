package com.HR.backend.controllers;

import com.HR.backend.models.Employee;
import com.HR.backend.services.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public List<Employee> searchEmployees(@RequestParam("cin") String cin) {
        return employeeService.searchEmployeesByCin(cin);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{cin}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Employee> updateEmployee(@PathVariable String cin, @Valid @RequestBody Employee employee) {
        Employee updated = employeeService.updateEmployee(cin, employee);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{cin}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String cin) {
        employeeService.deleteEmployee(cin);
        return ResponseEntity.noContent().build();
    }
}