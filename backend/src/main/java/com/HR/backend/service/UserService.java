package com.HR.backend.service;

import com.HR.backend.model.dto.EmployeeDTO;
import com.HR.backend.model.entity.Employee;
import com.HR.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    public EmployeeDTO getProfile() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        return employeeService.toDTO(employee);
    }

    public EmployeeDTO updateProfile(EmployeeDTO updatedEmployeeDTO) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        updatedEmployeeDTO.setCin(employee.getCin()); // Prevent CIN change
        return employeeService.updateEmployee(employee.getCin(), updatedEmployeeDTO);
    }
}