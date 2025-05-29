package com.HR.backend.service;

import com.HR.backend.model.dto.EmployeeDTO;
import com.HR.backend.model.entity.Department;
import com.HR.backend.model.entity.Employee;
import com.HR.backend.repository.DepartmentRepository;
import com.HR.backend.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<EmployeeDTO> searchEmployeesByCin(String cin) {
        return employeeRepository.findByCinContaining(cin).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        validateEmployee(employeeDTO);
        if (employeeRepository.existsByCin(employeeDTO.getCin())) {
            throw new IllegalArgumentException("Employee with CIN " + employeeDTO.getCin() + " already exists");
        }
        if (employeeRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new IllegalArgumentException("Email " + employeeDTO.getEmail() + " already in use");
        }
        Employee employee = toEntity(employeeDTO);
        return toDTO(employeeRepository.save(employee));
    }

    public EmployeeDTO updateEmployee(String cin, EmployeeDTO employeeDTO) {
        Employee existing = employeeRepository.findByCin(cin)
                .orElseThrow(() -> new EntityNotFoundException("Employee with CIN " + cin + " not found"));
        validateEmployee(employeeDTO);
        if (!employeeDTO.getEmail().equals(existing.getEmail()) && employeeRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new IllegalArgumentException("Email " + employeeDTO.getEmail() + " already in use");
        }
        Employee employee = toEntity(employeeDTO);
        employee.setCin(cin);
        return toDTO(employeeRepository.save(employee));
    }

    public void deleteEmployee(String cin) {
        Employee employee = employeeRepository.findByCin(cin)
                .orElseThrow(() -> new EntityNotFoundException("Employee with CIN " + cin + " not found"));
        employeeRepository.delete(employee);
    }

    private void validateEmployee(EmployeeDTO employeeDTO) {
        if (employeeDTO.getCin() == null || employeeDTO.getCin().isEmpty()) {
            throw new IllegalArgumentException("CIN is required");
        }
        if (employeeDTO.getEmail() == null || !employeeDTO.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (employeeDTO.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID is required");
        }
        departmentRepository.findById(employeeDTO.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        if (employeeDTO.getManagercin() != null && !employeeRepository.existsByCin(employeeDTO.getManagercin())) {
            throw new IllegalArgumentException("Manager with CIN " + employeeDTO.getManagercin() + " not found");
        }
    }

    public EmployeeDTO toDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setCin(employee.getCin());
        dto.setFirstname(employee.getFirstname());
        dto.setLastname(employee.getLastname());
        dto.setDateOfBirth(employee.getDateOfBirth());
        dto.setGender(employee.getGender());
        dto.setNationality(employee.getNationality());
        dto.setMaritalStatus(employee.getMaritalStatus());
        dto.setEmail(employee.getEmail());
        dto.setPhonenumber(employee.getPhonenumber());
        dto.setAddress(employee.getAddress());
        dto.setHiredate(employee.getHiredate());
        dto.setEmployeeStatus(employee.getEmployeeStatus());
        dto.setEmploymentType(employee.getEmploymentType());
        dto.setDepartmentId(employee.getDepartment().getId());
        dto.setJobtitle(employee.getJobtitle());
        dto.setSalary(employee.getSalary());
        dto.setManagercin(employee.getManagercin() != null ? employee.getManagercin().getCin() : null);
        dto.setCertifications(employee.getCertificationsList());
        dto.setPersonalLeaveBalance(employee.getPersonalLeaveBalance());
        return dto;
    }

    private Employee toEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setCin(dto.getCin());
        employee.setFirstname(dto.getFirstname());
        employee.setLastname(dto.getLastname());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setGender(dto.getGender());
        employee.setNationality(dto.getNationality());
        employee.setMaritalStatus(dto.getMaritalStatus());
        employee.setEmail(dto.getEmail());
        employee.setPhonenumber(dto.getPhonenumber());
        employee.setAddress(dto.getAddress());
        employee.setHiredate(dto.getHiredate());
        employee.setEmployeeStatus(dto.getEmployeeStatus());
        employee.setEmploymentType(dto.getEmploymentType());
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        employee.setDepartment(department);
        employee.setJobtitle(dto.getJobtitle());
        employee.setSalary(dto.getSalary());
        if (dto.getManagercin() != null) {
            Employee manager = employeeRepository.findByCin(dto.getManagercin())
                    .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
            employee.setManagercin(manager);
        }
        employee.setCertificationsList(dto.getCertifications());
        employee.setPersonalLeaveBalance(dto.getPersonalLeaveBalance());
        return employee;
    }
}
