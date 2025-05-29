package com.HR.backend.controller;

import com.HR.backend.model.dto.EmployeeDTO;
import com.HR.backend.service.EmployeeService;
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
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(@RequestParam("cin") String cin) {
        return ResponseEntity.ok(employeeService.searchEmployeesByCin(cin));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO created = employeeService.createEmployee(employeeDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{cin}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable String cin, @Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updated = employeeService.updateEmployee(cin, employeeDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{cin}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String cin) {
        employeeService.deleteEmployee(cin);
        return ResponseEntity.noContent().build();
    }
}