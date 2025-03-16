package com.HR.backend.services;

import com.HR.backend.models.Employee;
import com.HR.backend.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee addEmployee(Employee employee) {
        if (employeeRepository.existsByCin(employee.getCin())) {
            throw new IllegalArgumentException("Employee with CIN already exists");
        }
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(String cin, Employee updatedEmployee) {
        Employee existing = employeeRepository.findByCin(cin)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        existing.setDepartement(updatedEmployee.getDepartement());
        existing.setJobtitle(updatedEmployee.getJobtitle());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setPhonenumber(updatedEmployee.getPhonenumber());
        existing.setSalary(updatedEmployee.getSalary());
        existing.setManagernum(updatedEmployee.getManagernum());
        return employeeRepository.save(existing);
    }

    public void deleteEmployee(String cin) {
        if (!employeeRepository.existsByCin(cin)) {
            throw new IllegalArgumentException("Employee not found");
        }
        employeeRepository.deleteById(cin);
    }
}