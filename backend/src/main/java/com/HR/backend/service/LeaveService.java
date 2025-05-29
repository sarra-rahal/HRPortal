package com.HR.backend.service;

import com.HR.backend.model.dto.LeaveRequestDTO;
import com.HR.backend.model.entity.Employee;
import com.HR.backend.model.entity.LeaveRequest;
import com.HR.backend.model.entity.SickLeaveRequest;
import com.HR.backend.model.entity.AnnualLeaveRequest;
import com.HR.backend.model.entity.AnnualLeaveBalance;
import com.HR.backend.model.entity.User;
import com.HR.backend.repository.EmployeeRepository;
import com.HR.backend.repository.LeaveRequestRepository;
import com.HR.backend.repository.NotificationRepository;
import com.HR.backend.repository.SickLeaveRequestRepository;
import com.HR.backend.repository.UserRepository;
import com.HR.backend.repository.AnnualLeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final SickLeaveRequestRepository sickLeaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LeaveRequestDTO submitSickLeaveRequest(String email, LocalDate startDate, LocalDate endDate, String reason, MultipartFile certificate) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        validateLeaveRequest("SICK", startDate, endDate, employee, certificate);

        if (certificate == null || certificate.isEmpty()) {
            throw new IllegalArgumentException("Certificate is required for sick leave");
        }

        SickLeaveRequest sickRequest = new SickLeaveRequest();
        try {
            sickRequest.setCertificate(certificate.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to process certificate");
        }

        sickRequest.setEmployeeCin(employee.getCin());
        sickRequest.setLeaveType("SICK");
        sickRequest.setStartDate(startDate);
        sickRequest.setEndDate(endDate);
        sickRequest.setDuration((int) ChronoUnit.DAYS.between(startDate, endDate) + 1);
        sickRequest.setReason(reason);
        sickRequest.setSubmittedAt(LocalDateTime.now());
        sickRequest.setUpdatedAt(LocalDateTime.now());
        sickRequest = (SickLeaveRequest) leaveRequestRepository.save(sickRequest);

        sendNotification(email, "Sick leave request submitted");
        return convertToDTO(sickRequest);
    }

    public LeaveRequestDTO submitAnnualLeaveRequest(String email, LocalDate startDate, LocalDate endDate, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findByEmployeeCin(user.getCin());
        validateLeaveRequest("ANNUAL", startDate, endDate, employee, null);

        int duration = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (balance.getBalance() < duration) {
            throw new IllegalArgumentException("Insufficient annual leave balance");
        }

        AnnualLeaveRequest leaveRequest = new AnnualLeaveRequest();
        leaveRequest.setEmployeeCin(employee.getCin());
        leaveRequest.setLeaveType("ANNUAL");
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setDuration(duration);
        leaveRequest.setReason(reason);
        leaveRequest.setSubmittedAt(LocalDateTime.now());
        leaveRequest.setUpdatedAt(LocalDateTime.now());
        leaveRequest = (AnnualLeaveRequest) leaveRequestRepository.save(leaveRequest);

        sendNotification(email, "Annual leave request submitted");
        return convertToDTO(leaveRequest);
    }

    public LeaveRequestDTO updateSickLeaveRequest(Long id, String email, LocalDate startDate, LocalDate endDate, String reason, MultipartFile certificate) {
        LeaveRequest existingRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!(existingRequest instanceof SickLeaveRequest)) {
            throw new IllegalArgumentException("Request is not a sick leave request");
        }
        if (!existingRequest.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Only pending requests can be updated");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        validateLeaveRequest("SICK", startDate, endDate, employee, certificate);

        if (certificate != null && !certificate.isEmpty()) {
            try {
                ((SickLeaveRequest) existingRequest).setCertificate(certificate.getBytes());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to process certificate");
            }
        }

        existingRequest.setStartDate(startDate);
        existingRequest.setEndDate(endDate);
        existingRequest.setDuration((int) ChronoUnit.DAYS.between(startDate, endDate) + 1);
        existingRequest.setReason(reason);
        existingRequest.setUpdatedAt(LocalDateTime.now());
        leaveRequestRepository.save(existingRequest);

        sendNotification(email, "Sick leave request updated");
        return convertToDTO(existingRequest);
    }

    public LeaveRequestDTO updateAnnualLeaveRequest(Long id, String email, LocalDate startDate, LocalDate endDate, String reason) {
        LeaveRequest existingRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!(existingRequest instanceof AnnualLeaveRequest)) {
            throw new IllegalArgumentException("Request is not an annual leave request");
        }
        if (!existingRequest.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Only pending requests can be updated");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findByEmployeeCin(user.getCin());
        validateLeaveRequest("ANNUAL", startDate, endDate, employee, null);

        int duration = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (balance.getBalance() < duration) {
            throw new IllegalArgumentException("Insufficient annual leave balance");
        }

        existingRequest.setStartDate(startDate);
        existingRequest.setEndDate(endDate);
        existingRequest.setDuration(duration);
        existingRequest.setReason(reason);
        existingRequest.setUpdatedAt(LocalDateTime.now());
        leaveRequestRepository.save(existingRequest);

        sendNotification(email, "Annual leave request updated");
        return convertToDTO(existingRequest);
    }

    public void cancelLeaveRequest(Long id, String email) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (!request.getEmployeeCin().equals(employee.getCin())) {
            throw new IllegalArgumentException("Unauthorized to cancel this request");
        }
        if (!request.getStatus().equals("PENDING") && !request.getStatus().equals("DENIED")) {
            throw new IllegalArgumentException("Only pending or denied requests can be cancelled");
        }
        leaveRequestRepository.delete(request);
        sendNotification(email, "Leave request cancelled: " + request.getLeaveType());
    }

    public List<LeaveRequestDTO> getMyLeaveRequests(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        return leaveRequestRepository.findByEmployeeCin(employee.getCin()).stream()
                .map(this::convertToDTO)
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus("PENDING").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void reviewLeaveRequest(Long id, String status, String hrComment, String hrEmail) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!request.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Only pending requests can be reviewed");
        }
        request.setStatus(status.toUpperCase());
        request.setHrComment(hrComment);
        request.setUpdatedAt(LocalDateTime.now());
        if ("APPROVED".equalsIgnoreCase(status) && "ANNUAL".equalsIgnoreCase(request.getLeaveType())) {
            Employee employee = employeeRepository.findByCin(request.getEmployeeCin())
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
            AnnualLeaveBalance balance = annualLeaveBalanceRepository.findByEmployeeCin(employee.getCin());
            int newBalance = balance.getBalance() - request.getDuration();
            if (newBalance < 0) {
                throw new IllegalArgumentException("Insufficient leave balance");
            }
            balance.setBalance(newBalance);
            annualLeaveBalanceRepository.save(balance);
        }
        leaveRequestRepository.save(request);

        Employee employee = employeeRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        sendNotification(employee.getEmail(), "Leave request " + status.toLowerCase() + ": " + request.getLeaveType());
    }

    public void sendNote(Long id, String note, String hrEmail) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        request.setHrComment(note);
        request.setUpdatedAt(LocalDateTime.now());
        leaveRequestRepository.save(request);

        Employee employee = employeeRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        sendNotification(employee.getEmail(), "New note on leave request: " + note);
    }

    public Object getLeaveBalance(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findByEmployeeCin(employee.getCin());
        return new Object() {
            public int getAnnual() { return balance.getBalance(); }
            public int getSick() { return 999; } // Unlimited sick leave
        };
    }

    private void validateLeaveRequest(String leaveType, LocalDate startDate, LocalDate endDate, Employee employee, MultipartFile certificate) {
        if (!List.of("ANNUAL", "SICK").contains(leaveType.toUpperCase())) {
            throw new IllegalArgumentException("Invalid leave type");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    private LeaveRequestDTO convertToDTO(LeaveRequest request) {
        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setId(request.getId());
        dto.setEmployeeCin(request.getEmployeeCin());
        dto.setLeaveType(request.getLeaveType());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setDuration(request.getDuration());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus());
        dto.setHrComment(request.getHrComment());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        if (request instanceof SickLeaveRequest) {
            byte[] certificate = ((SickLeaveRequest) request).getCertificate();
            if (certificate != null) {
                dto.setCertificate(Base64.getEncoder().encodeToString(certificate));
            }
        }
        return dto;
    }

    private void sendNotification(String email, String message) {
        messagingTemplate.convertAndSendToUser(email, "/topic/notifications", message);
    }
}