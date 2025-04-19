package com.HR.backend.repositories;

import com.HR.backend.models.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    List<DocumentRequest> findByEmployeeEmailOrderByRequestDateDesc(String employeeEmail);
    List<DocumentRequest> findByStatusOrderByRequestDateDesc(String status);
}