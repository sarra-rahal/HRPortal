package com.HR.backend.controller;

import com.HR.backend.model.dto.EmployeeDTO;
import com.HR.backend.service.UserService;
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
    public ResponseEntity<EmployeeDTO> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/profile")
    public ResponseEntity<EmployeeDTO> updateProfile(@RequestBody EmployeeDTO updatedEmployeeDTO) {
        return ResponseEntity.ok(userService.updateProfile(updatedEmployeeDTO));
    }
}