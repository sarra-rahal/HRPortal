package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.Employee;
import com.HR.backend.models.LeaveRequest;
import com.HR.backend.models.User;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.LeaveRequestRepository;
import com.HR.backend.repositories.NotificationRepository;
import com.HR.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final String HR_EMAIL = "rahalsarra2@gmail.com";

    public LeaveRequest submitLeaveRequest(String employeeCin, LeaveRequest request, String token) {
        Employee employee = employeeRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        request.setDuration((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        request.setEmployeeCin(employeeCin);
        request.setStatus("PENDING");

        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        // Notify HR Manager
        String senderEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        String hrNotification = String.format(
                "New leave request from the employee %s %s with the cin %s ",
                employee.getFirstname(), employee.getLastname(), employee.getCin()
        );
        String hrEmail = String.format(
                """
                        Employee %s %s (%s) has submitted a leave request:
                        Type: %s
                        Dates: %s to %s
                        Reason: %s
                        Please review in the HR Portal.""",
                employee.getFirstname(), employee.getLastname(), employee.getCin(),
                request.getLeaveType(), request.getStartDate(), request.getEndDate(), request.getReason()
        );
        emailService.notifyUser(HR_EMAIL, hrNotification);
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrNotification);
        emailService.sendEmail(senderEmail, HR_EMAIL, "Leave request", hrEmail);

        return savedRequest;
    }

    public LeaveRequest cancelLeaveRequest(Long requestId, String employeeCin) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (!request.getEmployeeCin().equals(employeeCin)) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }
        leaveRequestRepository.delete(request);
        Employee employee = employeeRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));
        String hrNotification = String.format(
                "The leave request from the employee %s %s with the cin %s is canceled ",
                employee.getFirstname(), employee.getLastname(), employee.getCin()
        );
        emailService.notifyUser(HR_EMAIL, hrNotification);
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrNotification);
        return request;
    }

    public List<LeaveRequest> getEmployeeLeaveRequests(String employeeCin) {
        return leaveRequestRepository.findByEmployeeCin(employeeCin);
    }

    public Map<String, Integer> getLeaveBalance(String employeeCin) {
        Employee employee = employeeRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        Map<String, Integer> balance = new HashMap<>();
        balance.put("personal", employee.getPersonalLeaveBalance());
        balance.put("sick", employee.getSickLeaveBalance());
        balance.put("unpaid", employee.getUnpaidLeaveBalance());
        return balance;
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus("PENDING");
    }

    public LeaveRequest reviewLeaveRequest(Long requestId, String status, String hrComment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        Employee employee = employeeRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));

        if (status.equals("APPROVED")) {
            int duration = request.getDuration();
            switch (request.getLeaveType()) {
                case "ANNUAL":
                    if (employee.getPersonalLeaveBalance() < duration) {
                        throw new IllegalArgumentException("Insufficient personal leave balance");
                    }
                    employee.setPersonalLeaveBalance(employee.getPersonalLeaveBalance() - duration);
                    break;
                case "SICK":
                    if (employee.getSickLeaveBalance() < duration) {
                        throw new IllegalArgumentException("Insufficient sick leave balance");
                    }
                    employee.setSickLeaveBalance(employee.getSickLeaveBalance() - duration);
                    break;
                case "UNPAID":
                    if (employee.getUnpaidLeaveBalance() < duration) {
                        throw new IllegalArgumentException("Insufficient unpaid leave balance");
                    }
                    employee.setUnpaidLeaveBalance(employee.getUnpaidLeaveBalance() - duration);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid leave type");
            }
            employeeRepository.save(employee);
        }

        request.setStatus(status);
        request.setHrComment(hrComment);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest updatedRequest = leaveRequestRepository.save(request);

        User user = userRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));

        String employeeMessage = String.format(
                "Your leave request (%s, %s to %s) was %s. HR Comment: %s",
                request.getLeaveType(), request.getStartDate(), request.getEndDate(),
                status.toLowerCase(), hrComment != null ? hrComment : ""
        );
        emailService.notifyUser(user.getEmail(), employeeMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + user.getEmail(), employeeMessage);
        emailService.sendEmail(HR_EMAIL, user.getEmail(), "Leave Request " + request.getStatus(), String.format(
                "Your leave request (%s, %s to %s) has been %s.\nHR Comment: %s",
                updatedRequest.getLeaveType(), updatedRequest.getStartDate(), updatedRequest.getEndDate(),
                updatedRequest.getStatus().toLowerCase(), updatedRequest.getHrComment() != null ? request.getHrComment() : "None"
        ));
        return updatedRequest;
    }

    public LeaveRequest sendnote(Long requestId, String hrComment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        request.setHrComment(hrComment);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest updatedRequest = leaveRequestRepository.save(request);

        User user = userRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));

        Employee employee = employeeRepository.findByCin(request.getEmployeeCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee with this cin not found"));

        String employeeMessage = String.format(
                "the Humain ressources manager sent you a note about you leave request saying : %s",
                hrComment != null ? hrComment : ""
        );
        emailService.notifyUser(user.getEmail(), employeeMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + employee.getEmail(), employeeMessage);
        emailService.sendEmail(HR_EMAIL, user.getEmail(), "Note about your leave request", employeeMessage);
        return updatedRequest;
    }

    public LeaveRequest updateLeaveRequest(Long requestId, String employeeCin, LeaveRequest updatedRequest) {
        LeaveRequest existingRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        Employee employee = employeeRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        User user = userRepository.findByCin(employeeCin)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedRequest.getStartDate().isAfter(updatedRequest.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Update fields
        existingRequest.setLeaveType(updatedRequest.getLeaveType());
        existingRequest.setStatus("PENDING");
        existingRequest.setStartDate(updatedRequest.getStartDate());
        existingRequest.setEndDate(updatedRequest.getEndDate());
        existingRequest.setDuration((int) ChronoUnit.DAYS.between(updatedRequest.getStartDate(), updatedRequest.getEndDate()) + 1);
        existingRequest.setReason(updatedRequest.getReason());
        existingRequest.setUpdatedAt(LocalDateTime.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(existingRequest);

        // Notify HR of the update
        String hrMessage = String.format(
                "the employee %s %s with cin %s updated his leave request",
                employee.getCin(), employee.getFirstname(), employee.getLastname()
        );

        emailService.notifyUser(HR_EMAIL, hrMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrMessage);
        emailService.sendEmail(user.getEmail(), HR_EMAIL, "Leave Request Updated", String.format(
                """
                        Employee %s %s (%s) has updated his leave request:
                        Type: %s
                        Dates: %s to %s
                        Reason: %s
                        Please review in the HR Portal.""",
                employee.getFirstname(), employee.getLastname(), employee.getCin(),
                savedRequest.getLeaveType(), savedRequest.getStartDate(), savedRequest.getEndDate(), savedRequest.getReason()
        ));
        return savedRequest;
    }
}