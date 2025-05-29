package com.HR.backend.repository;

import com.HR.backend.model.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeCin(String employeeCin);
    List<LeaveRequest> findByStatus(String status);
    List<LeaveRequest> findByLeaveTypeAndEmployeeCin(String leaveType, String employeeCin);
    List<LeaveRequest> findByLeaveTypeAndStatus(String leaveType, String status);
}