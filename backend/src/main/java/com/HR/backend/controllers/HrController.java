package com.HR.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@PreAuthorize("hasAuthority('HR_MANAGER')")
public class HrController {

    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, String>>> getEmployees() {
        // Placeholder data; replace with real DB logic
        List<Map<String, String>> employees = List.of(
                Map.of("cin", "10000001", "jobtitle", "HR Manager"),
                Map.of("cin", "10000002", "jobtitle", "Software Engineer")
        );
        return ResponseEntity.ok(employees);
    }
}