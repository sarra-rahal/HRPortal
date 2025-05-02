package com.HR.backend.services;

import com.HR.backend.models.Employee;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private static final Pattern CIN_PATTERN = Pattern.compile("^[0-9]{8}$");

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee createEmployee(Employee employee) {
        validateEmployee(employee);
        if (employeeRepository.existsById(employee.getCin())) {
            throw new IllegalArgumentException("Employee with CIN " + employee.getCin() + " already exists");
        }
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email " + employee.getEmail() + " is already in use");
        }
        if (!departmentRepository.existsById(employee.getDepartment().getId())) {
            throw new IllegalArgumentException("Department with ID " + employee.getDepartment().getId() + " does not exist");
        }
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(String cin, Employee employee) {
        Employee existing = employeeRepository.findById(cin)
                .orElseThrow(() -> new EntityNotFoundException("Employee with CIN " + cin + " not found"));
        employee.setCin(cin); // Ensure CIN is not changed
        validateEmployee(employee);
        if (!employee.getEmail().equals(existing.getEmail()) && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email " + employee.getEmail() + " is already in use");
        }
        if (!departmentRepository.existsById(employee.getDepartment().getId())) {
            throw new IllegalArgumentException("Department with ID " + employee.getDepartment().getId() + " does not exist");
        }
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(String cin) {
        if (!employeeRepository.existsById(cin)) {
            throw new EntityNotFoundException("Employee with CIN " + cin + " not found");
        }
        employeeRepository.deleteById(cin);
    }

    private void validateEmployee(Employee employee) {
        if (!CIN_PATTERN.matcher(employee.getCin()).matches()) {
            throw new IllegalArgumentException("CIN must be 8 digits");
        }
        if (employee.getFirstname() == null || employee.getFirstname().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (employee.getLastname() == null || employee.getLastname().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (employee.getEmail() == null || !employee.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (employee.getHiredate() == null || employee.getHiredate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Valid hire date is required and cannot be in the future");
        }
        if (employee.getJobtitle() == null || employee.getJobtitle().isEmpty()) {
            throw new IllegalArgumentException("Job title is required");
        }
        if (employee.getDepartment() == null || employee.getDepartment().getId() == null) {
            throw new IllegalArgumentException("Department is required");
        }
        if (employee.getSalary() == null || employee.getSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid salary is required");
        }
    }
}