package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.*;
import com.HR.backend.repositories.*;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final TrainingEnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private static final String HR_EMAIL = "rahalsarra2@gmail.com";

    public List<Training> getAllTrainings() {
        return trainingRepository.findAll();
    }

    public List<TrainingEnrollment> getPendingEnrollments() {
        return enrollmentRepository.findByStatus("PENDING");
    }

    @Transactional
    public Training createTraining(Training training, String token) {
        validateTraining(training);
        Training saved = trainingRepository.save(training);

        List<User> users = userRepository.findAllByRoleNot(User.UserRole.HR_MANAGER);
        for (User user : users) {
            if (!Objects.equals(String.valueOf(user.getRole()), "HR_MANAGER")) {
                String message = "New training available: " + saved.getTitle();
                emailService.notifyUser(user.getEmail(), message);
                messagingTemplate.convertAndSend("/topic/notifications/" + user.getEmail(), message);
            }
        }
        return saved;
    }

    @Transactional
    public Training updateTraining(Training training, String token) {
        Training existing = trainingRepository.findById(training.getId())
                .orElseThrow(() -> new RuntimeException("Training not found"));
        validateTraining(training);

        existing.setTitle(training.getTitle());
        existing.setDescription(training.getDescription());
        existing.setStartDate(training.getStartDate());
        existing.setEndDate(training.getEndDate());
        existing.setCapacity(training.getCapacity());
        existing.setCategory(training.getCategory());
        existing.setRating(training.getRating());

        Training updated = trainingRepository.save(existing);

        List<TrainingEnrollment> enrollments = enrollmentRepository.findByTrainingId(training.getId());
        for (TrainingEnrollment enrollment : enrollments) {
            String message = String.format("Training %s has been updated", updated.getTitle());
            emailService.notifyUser(enrollment.getEmployeeEmail(), message);
            messagingTemplate.convertAndSend("/topic/notifications/" + enrollment.getEmployeeEmail(), message);
        }

        return updated;
    }

    @Transactional
    public void deleteTraining(Long trainingId, String token) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Training not found"));

        List<TrainingEnrollment> enrollments = enrollmentRepository.findByTrainingId(trainingId);
        for (TrainingEnrollment enrollment : enrollments) {
            String message = String.format("Training %s has been cancelled", training.getTitle());
            emailService.notifyUser(enrollment.getEmployeeEmail(), message);
            messagingTemplate.convertAndSend("/topic/notifications/" + enrollment.getEmployeeEmail(), message);
            enrollmentRepository.delete(enrollment);
        }

        trainingRepository.delete(training);
    }

    @Transactional
    public TrainingEnrollment enroll(Long trainingId, String employeeEmail, String token) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Training not found"));

        if (training.getEnrolledCount() >= training.getCapacity()) {
            throw new RuntimeException("Training capacity full");
        }
        if (!LocalDateTime.now().isBefore(training.getStartDate())) {
            throw new RuntimeException("Cannot enroll in past or ongoing training");
        }

        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setTraining(training);
        enrollment.setEmployeeEmail(employeeEmail);
        enrollment.setStatus("PENDING");
        TrainingEnrollment saved = enrollmentRepository.save(enrollment);

        String senderEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        User user = userRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        if (employeeEmail == null || employeeEmail.isEmpty()) {
            throw new IllegalArgumentException("Employee email cannot be null or empty");
        }

        String hrMessage = String.format(
                "%s %s has submitted an enrollment request for %s",
                employee.getFirstname(), employee.getLastname(), training.getTitle()
        );
        emailService.notifyUser(HR_EMAIL, hrMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrMessage);
        emailService.sendEmail(senderEmail, HR_EMAIL, "Training Enrollment Request", hrMessage);
        return saved;
    }

    @Transactional
    public TrainingEnrollment reviewEnrollment(Long enrollmentId, String status, String token) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus(status);
        TrainingEnrollment updated = enrollmentRepository.save(enrollment);

        if (status.equals("APPROVED")) {
            Training training = enrollment.getTraining();
            training.setEnrolledCount(training.getEnrolledCount() + 1);
            trainingRepository.save(training);
        }

        String hrEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        String trainingTitle = enrollment.getTraining().getTitle();
        String employeeMessage = String.format(
                "Your enrollment for %s has been %s%s",
                trainingTitle,
                status.toLowerCase(),
                status.equals("APPROVED") ? ". You are now enrolled in the training named " + trainingTitle : "."
        );
        emailService.notifyUser(enrollment.getEmployeeEmail(), employeeMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + enrollment.getEmployeeEmail(), updated);
        emailService.sendEmail(
                hrEmail,
                enrollment.getEmployeeEmail(),
                "Enrollment Update",
                employeeMessage
        );
        return updated;
    }

    public List<TrainingEnrollment> getEmployeeEnrollments(String employeeEmail) {
        return enrollmentRepository.findByEmployeeEmail(employeeEmail);
    }

    private void validateTraining(Training training) {
        if (training.getStartDate().isAfter(training.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (training.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (training.getCategory() == null || training.getCategory().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
    }

    @Transactional
    public void cancelEnrollment(Long enrollmentId, String employeeEmail) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (!enrollment.getStatus().equals("PENDING")) {
            throw new RuntimeException("Can only cancel pending enrollments");
        }

        Training training = enrollment.getTraining();
        enrollmentRepository.delete(enrollment);
        emailService.notifyUser(HR_EMAIL, "Enrollment cancelled for " + training.getTitle());
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL,
                "Enrollment cancelled for " + training.getTitle());
    }
}