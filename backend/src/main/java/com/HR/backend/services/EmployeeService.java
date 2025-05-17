package com.HR.backend.services;

import com.HR.backend.models.Employee;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private static final Pattern CIN_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<Employee> searchEmployeesByCin(String cin) {
        return employeeRepository.findByCinContaining(cin);
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
        if (employee.getManagercin() != null && !employeeRepository.existsById(employee.getManagercin().getCin())) {
            throw new IllegalArgumentException("Manager with CIN " + employee.getManagercin().getCin() + " does not exist");
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
        if (employee.getManagercin() != null && !employeeRepository.existsById(employee.getManagercin().getCin())) {
            throw new IllegalArgumentException("Manager with CIN " + employee.getManagercin().getCin() + " does not exist");
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
        if (employee.getEmail() == null || !EMAIL_PATTERN.matcher(employee.getEmail()).matches()) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (employee.getPhonenumber() == null || !PHONE_PATTERN.matcher(employee.getPhonenumber()).matches()) {
            throw new IllegalArgumentException("Valid phone number is required");
        }
        if (employee.getAddress() == null || employee.getAddress().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (employee.getHiredate() == null || employee.getHiredate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Valid hire date is required");
        }
        if (employee.getGender() == null || !Arrays.asList("Male", "Female").contains(employee.getGender())) {
            throw new IllegalArgumentException("Gender must be Male or Female");
        }
        if (employee.getNationality() == null || employee.getNationality().isEmpty()) {
            throw new IllegalArgumentException("Nationality is required");
        }
        if (employee.getMaritalStatus() == null || !Arrays.asList("Single", "Married", "Divorced").contains(employee.getMaritalStatus())) {
            throw new IllegalArgumentException("Marital status must be Single, Married, or Divorced");
        }
        if (employee.getEmployeeStatus() == null || !Arrays.asList("Active", "On Leave").contains(employee.getEmployeeStatus())) {
            throw new IllegalArgumentException("Employee status must be Active or On Leave");
        }
        if (employee.getEmploymentType() == null || !Arrays.asList("Full-Time", "Part-Time", "Contractor").contains(employee.getEmploymentType())) {
            throw new IllegalArgumentException("Employment type must be Full-Time, Part-Time, or Contractor");
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
        if (employee.getCertifications() == null || employee.getCertifications().isEmpty()) {
            throw new IllegalArgumentException("Certifications cannot be empty");
        }
        if (employee.getPersonalLeaveBalance() == null || employee.getPersonalLeaveBalance() < 0) {
            throw new IllegalArgumentException("Personal leave balance must be non-negative");
        }
        if (employee.getSickLeaveBalance() == null || employee.getSickLeaveBalance() < 0) {
            throw new IllegalArgumentException("Sick leave balance must be non-negative");
        }
        if (employee.getUnpaidLeaveBalance() == null || employee.getUnpaidLeaveBalance() < 0) {
            throw new IllegalArgumentException("Unpaid leave balance must be non-negative");
        }
    }
}