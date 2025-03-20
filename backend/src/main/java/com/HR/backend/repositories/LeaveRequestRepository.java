package com.HR.backend.repositories;

import com.HR.backend.models.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeCin(String employeeCin);
    List<LeaveRequest> findByStatus(String status); // For HR dashboard
}