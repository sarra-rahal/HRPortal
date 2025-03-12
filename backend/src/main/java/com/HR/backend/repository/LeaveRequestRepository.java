package com.HR.backend.repository;

import com.HR.backend.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeCin(String employeeCin);
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
}