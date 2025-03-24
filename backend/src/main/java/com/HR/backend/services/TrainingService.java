package com.HR.backend.services;
import com.HR.backend.config.JwtUtil;
import com.HR.backend.models.Employee;
import com.HR.backend.models.Notification;
import com.HR.backend.models.TrainingEnrollment;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.NotificationRepository;
import com.HR.backend.repositories.TrainingEnrollmentRepository;
import com.HR.backend.repositories.TrainingRepository;
import com.HR.backend.models.Training;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final TrainingEnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private static final String HR_EMAIL = "rahalsarra2@gmail.com";

    public List<Training> getAllTrainings() {
        return trainingRepository.findAll();
    }

    public Training createTraining(Training training, String token) {
        Training saved = trainingRepository.save(training);
        String hrEmail = jwtUtil.extractEmail(token.split(" ")[1]); // Extract from Bearer token
        notifyUser(hrEmail, "New training created: " + saved.getTitle());
        messagingTemplate.convertAndSend("/topic/trainings", saved);
        return saved;
    }

    @Transactional
    public TrainingEnrollment enroll(Long trainingId, String employeeEmail, String token) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Training not found"));

        if (training.getEnrolledCount() >= training.getCapacity()) {
            throw new RuntimeException("Training capacity full");
        }

        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setTraining(training);
        enrollment.setEmployeeEmail(employeeEmail);
        enrollment.setStatus("PENDING");
        TrainingEnrollment saved = enrollmentRepository.save(enrollment);

        training.setEnrolledCount(training.getEnrolledCount() + 1);
        trainingRepository.save(training);

        String senderEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        if (employeeEmail == null || employeeEmail.isEmpty()) {
            throw new IllegalArgumentException("Employee email cannot be null or empty");
        }
        String hrMessage = String.format(
                "%s %s (%s) enrolled in training: %s",
                employee.getFirstname(), employee.getLastname(), employee.getCin(), training.getTitle()
        );
        notifyUser(employeeEmail, "You enrolled in " + training.getTitle());
        notifyUser(HR_EMAIL, hrMessage); // Notify HR Manager
        messagingTemplate.convertAndSend("/topic/notifications/" + employeeEmail, saved);
        messagingTemplate.convertAndSend("/topic/notifications/" + HR_EMAIL, hrMessage);
        emailService.sendEmail(senderEmail, HR_EMAIL, "Training Enrollment", hrMessage);
        return saved;
    }

    @Transactional
    public TrainingEnrollment reviewEnrollment(Long enrollmentId, String status, String token) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus(status);
        TrainingEnrollment updated = enrollmentRepository.save(enrollment);

        String hrEmail = jwtUtil.extractEmail(token.split(" ")[1]);
        String employeeMessage = "Your enrollment for " + enrollment.getTraining().getTitle() + " was " + status.toLowerCase();
        notifyUser(enrollment.getEmployeeEmail(), employeeMessage);
        messagingTemplate.convertAndSend("/topic/notifications/" + enrollment.getEmployeeEmail(), updated);
        emailService.sendEmail(hrEmail, enrollment.getEmployeeEmail(), "Enrollment Update",
                "Your enrollment has been " + status.toLowerCase() + ".");
        return updated;
    }

    public List<TrainingEnrollment> getEmployeeEnrollments(String employeeEmail) {
        return enrollmentRepository.findByEmployeeEmail(employeeEmail);
    }

    private void notifyUser(String email, String message) {
        Notification notification = new Notification();
        notification.setUserEmail(email);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }
}