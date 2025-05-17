package com.HR.backend.controllers;

import com.HR.backend.models.Employee;
import com.HR.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<Employee> getProfile() {
        Employee employee = userService.getProfile();
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/profile")
    public ResponseEntity<Employee> updateProfile(@RequestBody Employee updatedEmployee) {
        Employee employee = userService.updateProfile(updatedEmployee);
        return ResponseEntity.ok(employee);
    }
}