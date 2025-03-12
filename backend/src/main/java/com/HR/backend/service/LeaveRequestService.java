package com.HR.backend.service;

import com.HR.backend.model.LeaveRequest;
import com.HR.backend.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequest submitLeaveRequest(LeaveRequest request) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Assume CIN is linked to email in real app; for now, set manually
        request.setEmployeeCin(request.getEmployeeCin()); // Replace with actual logic
        return leaveRequestRepository.save(request);
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING);
    }

    public List<LeaveRequest> getEmployeeLeaveRequests(String employeeCin) {
        return leaveRequestRepository.findByEmployeeCin(employeeCin);
    }

    public LeaveRequest updateLeaveRequestStatus(Long id, LeaveRequest.LeaveStatus status) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        request.setStatus(status);
        return leaveRequestRepository.save(request);
    }
}