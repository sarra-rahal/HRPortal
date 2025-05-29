package com.HR.backend.repository;

import com.HR.backend.model.entity.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    List<DocumentRequest> findByEmployeeEmailOrderByRequestDateDesc(String employeeEmail);
    List<DocumentRequest> findByStatusInOrderByRequestDateDesc(List<String> status);
}