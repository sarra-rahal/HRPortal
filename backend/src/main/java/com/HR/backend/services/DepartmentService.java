package com.HR.backend.services;

import com.HR.backend.models.Department;
import com.HR.backend.repositories.DepartmentRepository;
import com.HR.backend.repositories.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department createDepartment(Department department) {
        validateDepartment(department);
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Department with name " + department.getName() + " already exists");
        }
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Long id, Department department) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department with ID " + id + " not found"));
        validateDepartment(department);
        if (!department.getName().equals(existing.getName()) && departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Department with name " + department.getName() + " already exists");
        }
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Long id) {
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department with assigned employees");
        }
        departmentRepository.deleteById(id);
    }

    private void validateDepartment(Department department) {
        if (department.getName() == null || department.getName().isEmpty()) {
            throw new IllegalArgumentException("Department name is required");
        }
        if (department.getName().length() > 100) {
            throw new IllegalArgumentException("Department name cannot exceed 100 characters");
        }
    }
}