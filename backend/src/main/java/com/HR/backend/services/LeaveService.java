package com.HR.backend.services;

import com.HR.backend.models.Employee;
import com.HR.backend.models.LeaveRequest;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String HR_EMAIL = "rahalsarra2@gmail.com";

    public LeaveRequest submitLeaveRequest(String employeeCin, LeaveRequest request) {
        Employee employee = employeeRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<LeaveRequest> existingRequests = leaveRequestRepository.findByEmployeeCin(employeeCin);
        for (LeaveRequest existing : existingRequests) {
            if (request.getStartDate().isBefore(existing.getEndDate()) &&
                    request.getEndDate().isAfter(existing.getStartDate()) &&
                    !existing.getStatus().equals("DENIED")) {
                throw new IllegalArgumentException("Leave request overlaps with an existing request");
            }
        }

        request.setDuration((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        request.setEmployeeCin(employeeCin);
        request.setStatus("PENDING");

        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        //notify
        String hrMessage = String.format(
                "New leave request from %s %s: %s (%s to %s)",
                employee.getFirstname(), employee.getLastname(), request.getLeaveType(),
                request.getStartDate(), request.getEndDate()
        );
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrMessage);
        notifyHrManager(employee, savedRequest);

        return savedRequest;
    }

    public LeaveRequest cancelLeaveRequest(Long requestId, String employeeCin) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!request.getEmployeeCin().equals(employeeCin)) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }
        if (!request.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Only pending requests can be canceled");
        }
        leaveRequestRepository.delete(request);
        return request;
    }

    public List<LeaveRequest> getEmployeeLeaveRequests(String employeeCin) {
        return leaveRequestRepository.findByEmployeeCin(employeeCin);
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus("PENDING");
    }

    public LeaveRequest reviewLeaveRequest(Long requestId, String status, String hrComment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        request.setStatus(status);
        request.setHrComment(hrComment);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest updatedRequest = leaveRequestRepository.save(request);

        Employee employee = employeeRepository.findByCin(request.getEmployeeCin()).orElse(null);
        if (employee != null) {
            String employeeMessage = String.format(
                    "Your leave request (%s, %s to %s) was %s. HR Comment: %s",
                    request.getLeaveType(), request.getStartDate(), request.getEndDate(),
                    status.toLowerCase(), hrComment != null ? hrComment : "None"
            );
            messagingTemplate.convertAndSend("/topic/notifications/" + employee.getEmail(), employeeMessage);
            notifyEmployee(updatedRequest);
        }

        return updatedRequest;
    }

    private void notifyHrManager(Employee employee, LeaveRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(employee.getEmail());
        message.setTo(HR_EMAIL);
        message.setSubject("New Leave Request Submitted");
        message.setText(String.format(
                """
                        Employee %s %s (%s) has submitted a leave request:
                        Type: %s
                        Dates: %s to %s
                        Reason: %s
                        Please review in the HR Portal.""",
                employee.getFirstname(), employee.getLastname(), employee.getCin(),
                request.getLeaveType(), request.getStartDate(), request.getEndDate(), request.getReason()
        ));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.error("Failed to send HR notification for leave request ID {}: {}", request.getId(), e.getMessage());
        }
    }

    private void notifyEmployee(LeaveRequest request) {
        Employee employee = employeeRepository.findByCin(request.getEmployeeCin()).orElse(null);
        if (employee != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(HR_EMAIL);
            message.setTo(employee.getEmail());
            message.setSubject("Leave Request " + request.getStatus());
            message.setText(String.format(
                    "Your leave request (%s, %s to %s) has been %s.\nHR Comment: %s",
                    request.getLeaveType(), request.getStartDate(), request.getEndDate(),
                    request.getStatus().toLowerCase(), request.getHrComment() != null ? request.getHrComment() : "None"
            ));
            mailSender.send(message);
        }
    }
}