package com.HR.backend.service;

import com.HR.backend.model.dto.DepartmentDTO;
import com.HR.backend.model.entity.Department;
import com.HR.backend.repository.DepartmentRepository;
import com.HR.backend.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        validateDepartment(departmentDTO);
        if (departmentRepository.existsByName(departmentDTO.getName())) {
            throw new IllegalArgumentException("Department with name " + departmentDTO.getName() + " already exists");
        }
        Department department = toEntity(departmentDTO);
        return toDTO(departmentRepository.save(department));
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department with ID " + id + " not found"));
        validateDepartment(departmentDTO);
        if (!departmentDTO.getName().equals(existing.getName()) && departmentRepository.existsByName(departmentDTO.getName())) {
            throw new IllegalArgumentException("Department with name " + departmentDTO.getName() + " already exists");
        }
        Department department = toEntity(departmentDTO);
        department.setId(id);
        return toDTO(departmentRepository.save(department));
    }

    public void deleteDepartment(Long id) {
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department with assigned employees");
        }
        departmentRepository.deleteById(id);
    }

    private void validateDepartment(DepartmentDTO departmentDTO) {
        if (departmentDTO.getName() == null || departmentDTO.getName().isEmpty()) {
            throw new IllegalArgumentException("Department name is required");
        }
        if (departmentDTO.getName().length() > 100) {
            throw new IllegalArgumentException("Department name cannot exceed 100 characters");
        }
    }

    private DepartmentDTO toDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        return dto;
    }

    private Department toEntity(DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        return department;
    }
}