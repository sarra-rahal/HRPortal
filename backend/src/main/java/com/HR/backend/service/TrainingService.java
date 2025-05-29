package com.HR.backend.service;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.model.dto.TrainingDTO;
import com.HR.backend.model.dto.TrainingEnrollmentDTO;
import com.HR.backend.model.dto.TrainingProposalDTO;
import com.HR.backend.model.entity.*;
import com.HR.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final TrainingProposalRepository trainingProposalRepository;
    private final TrainingEnrollmentRepository enrollmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;
    private final SimpMessagingTemplate messagingTemplate;

    public List<TrainingDTO> getAllTrainings() {
        return trainingRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TrainingDTO createTraining(TrainingDTO trainingDTO, String token) {
        if (!isHrManager(token)) {
            throw new IllegalArgumentException("Only HR Managers can create trainings");
        }
        validateTraining(trainingDTO);
        Training training = toEntity(trainingDTO);
        training = trainingRepository.save(training);

        sendNotification(jwtUtil.extractEmail(token.split(" ")[1]), "Training created: " + training.getTitle());
        notifyEmployees("New training available: " + training.getTitle());
        return toDTO(training);
    }

    public TrainingDTO updateTraining(Long trainingId, TrainingDTO trainingDTO, String token) {
        if (!isHrManager(token)) {
            throw new IllegalArgumentException("Only HR Managers can update trainings");
        }
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new IllegalArgumentException("Training not found"));
        validateTraining(trainingDTO);
        updateEntity(training, trainingDTO);
        training = trainingRepository.save(training);

        sendNotification(jwtUtil.extractEmail(token.split(" ")[1]), "Training updated: " + training.getTitle());
        return toDTO(training);
    }

    public void deleteTraining(Long trainingId, String token) {
        if (!isHrManager(token)) {
            throw new IllegalArgumentException("Only HR Managers can delete trainings");
        }
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new IllegalArgumentException("Training not found"));
        if (!enrollmentRepository.findByTrainingId(trainingId).isEmpty()) {
            throw new IllegalStateException("Cannot delete training with enrollments");
        }
        trainingRepository.delete(training);
        sendNotification(jwtUtil.extractEmail(token.split(" ")[1]), "Training deleted: " + training.getTitle());
    }

    public TrainingEnrollmentDTO enroll(Long trainingId, String email, String token) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new IllegalArgumentException("Training not found"));
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (training.getEnrolledCount() >= training.getCapacity()) {
            throw new IllegalStateException("Training is full");
        }
        if (enrollmentRepository.findByTrainingId(trainingId).stream()
                .anyMatch(e -> e.getEmployeeEmail().equals(email))) {
            throw new IllegalStateException("You are already enrolled in this training");
        }
        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setTraining(training);
        enrollment.setEmployeeEmail(email);
        enrollment.setStatus("PENDING");
        enrollment = enrollmentRepository.save(enrollment);

        training.setEnrolledCount(training.getEnrolledCount() + 1);
        trainingRepository.save(training);

        sendNotification(email, "Enrollment request submitted for: " + training.getTitle());
        notifyHrManagers("New enrollment request from " + employee.getFirstname() + " " + employee.getLastname());
        return toEnrollmentDTO(enrollment);
    }

    public void cancelEnrollment(Long enrollmentId, String email) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        if (!enrollment.getEmployeeEmail().equals(email)) {
            throw new IllegalArgumentException("You can only cancel your own enrollments");
        }
        if (!enrollment.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Only pending enrollments can be cancelled");
        }
        Training training = enrollment.getTraining();
        training.setEnrolledCount(training.getEnrolledCount() - 1);
        trainingRepository.save(training);
        enrollmentRepository.delete(enrollment);
        sendNotification(email, "Enrollment cancelled for: " + training.getTitle());
    }

    public TrainingEnrollmentDTO reviewEnrollment(Long enrollmentId, String status, String token) {
        if (!isHrManager(token)) {
            throw new IllegalArgumentException("Only HR Managers can review enrollments");
        }
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        if (!Arrays.asList("APPROVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Invalid status");
        }
        enrollment.setStatus(status);
        enrollment = enrollmentRepository.save(enrollment);

        if ("REJECTED".equals(status)) {
            Training training = enrollment.getTraining();
            training.setEnrolledCount(training.getEnrolledCount() - 1);
            trainingRepository.save(training);
        }
        sendNotification(enrollment.getEmployeeEmail(), "Enrollment " + status.toLowerCase() + " for: " + enrollment.getTraining().getTitle());
        return toEnrollmentDTO(enrollment);
    }

    public List<TrainingEnrollmentDTO> getEmployeeEnrollments(String email) {
        return enrollmentRepository.findByEmployeeEmail(email)
                .stream()
                .map(this::toEnrollmentDTO)
                .collect(Collectors.toList());
    }

    public List<TrainingEnrollmentDTO> getPendingEnrollments() {
        return enrollmentRepository.findByStatus("PENDING")
                .stream()
                .map(this::toEnrollmentDTO)
                .collect(Collectors.toList());
    }

    private void validateTraining(TrainingDTO trainingDTO) {
        if (trainingDTO.getTitle() == null || trainingDTO.getTitle().isEmpty()) {
            throw new IllegalArgumentException("Training title is required");
        }
        if (trainingDTO.getStartDate() == null || trainingDTO.getEndDate() == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (trainingDTO.getStartDate().isAfter(trainingDTO.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (trainingDTO.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
    }

    private boolean isHrManager(String token) {
        String role = jwtUtil.extractRole(token.split(" ")[1]);
        return "HR_MANAGER".equals(role);
    }

    private void sendNotification(String email, String message) {
        NotificationService.sendNotification(email, message, notificationRepository, messagingTemplate);
    }

    private void notifyHrManagers(String message) {
        List<User> hrManagers = userRepository.findAllByRoleNot(User.UserRole.EMPLOYEE);
        for (User hr : hrManagers) {
            sendNotification(hr.getEmail(), message);
        }
    }

    private void notifyEmployees(String message) {
        List<User> employees = userRepository.findAllByRoleNot(User.UserRole.HR_MANAGER);
        for (User emp : employees) {
            sendNotification(emp.getEmail(), message);
        }
    }

    private TrainingDTO toDTO(Training training) {
        TrainingDTO dto = new TrainingDTO();
        dto.setId(training.getId());
        dto.setTitle(training.getTitle());
        dto.setDescription(training.getDescription());
        dto.setStartDate(training.getStartDate());
        dto.setEndDate(training.getEndDate());
        dto.setCapacity(training.getCapacity());
        dto.setEnrolledCount(training.getEnrolledCount());
        dto.setCategory(training.getCategory());
        dto.setRating(training.getRating());
        dto.setHostCompany(training.getHostCompany());
        dto.setType(training.getType());
        return dto;
    }

    private Training toEntity(TrainingDTO dto) {
        Training training = new Training();
        training.setTitle(dto.getTitle());
        training.setDescription(dto.getDescription());
        training.setStartDate(dto.getStartDate());
        training.setEndDate(dto.getEndDate());
        training.setCapacity(dto.getCapacity());
        training.setEnrolledCount(0);
        training.setCategory(dto.getCategory());
        training.setRating(dto.getRating());
        training.setHostCompany(dto.getHostCompany());
        training.setType(dto.getType());
        return training;
    }

    private void updateEntity(Training training, TrainingDTO dto) {
        training.setTitle(dto.getTitle());
        training.setDescription(dto.getDescription());
        training.setStartDate(dto.getStartDate());
        training.setEndDate(dto.getEndDate());
        training.setCapacity(dto.getCapacity());
        training.setCategory(dto.getCategory());
        training.setRating(dto.getRating());
        training.setHostCompany(dto.getHostCompany());
        training.setType(dto.getType());
    }

    private TrainingEnrollmentDTO toEnrollmentDTO(TrainingEnrollment enrollment) {
        TrainingEnrollmentDTO dto = new TrainingEnrollmentDTO();
        dto.setId(enrollment.getId());
        dto.setTraining(toDTO(enrollment.getTraining()));
        dto.setEmployeeEmail(enrollment.getEmployeeEmail());
        dto.setStatus(enrollment.getStatus());
        return dto;
    }
    public TrainingProposalDTO proposeTraining(TrainingProposalDTO proposalDTO, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        TrainingProposal proposal = toProposalEntity(proposalDTO);
        proposal.setProposedBy(email);
        proposal.setStatus("PENDING");
        proposal = trainingProposalRepository.save(proposal);
        notifyHrManagers("New training proposal from " + email);
        return toProposalDTO(proposal);
    }

    public TrainingProposalDTO updateTrainingProposal(Long proposalId, TrainingProposalDTO proposalDTO, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        TrainingProposal proposal = trainingProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Training proposal not found"));
        if (!proposal.getProposedBy().equals(email)) {
            throw new IllegalArgumentException("You can only edit your own proposals");
        }
        if (!"PENDING".equals(proposal.getStatus())) {
            throw new IllegalArgumentException("Only pending proposals can be edited");
        }
        updateProposalEntity(proposal, proposalDTO);
        proposal = trainingProposalRepository.save(proposal);
        sendNotification("rahalsarra2@gmail.com", "Training proposal updated: " + proposal.getTitle());
        return toProposalDTO(proposal);
    }

    public void cancelTrainingProposal(Long proposalId, String token) {
        String email = jwtUtil.extractEmail(token.split(" ")[1]);
        TrainingProposal proposal = trainingProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Training proposal not found"));
        if (!proposal.getProposedBy().equals(email)) {
            throw new IllegalArgumentException("You can only cancel your own proposals");
        }
        if (!"PENDING".equals(proposal.getStatus())) {
            throw new IllegalArgumentException("Only pending proposals can be cancelled");
        }
        trainingProposalRepository.delete(proposal);
        sendNotification("rahalsarra2@gmail.com", "Training proposal cancelled: " + proposal.getTitle());
    }

    public TrainingProposalDTO reviewTrainingProposal(Long proposalId, String status, String token) {
        if (!isHrManager(token)) {
            throw new IllegalArgumentException("Only HR Managers can review proposals");
        }
        TrainingProposal proposal = trainingProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Training proposal not found"));
        if (!Arrays.asList("APPROVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Invalid status");
        }
        proposal.setStatus(status);
        proposal = trainingProposalRepository.save(proposal);
        sendNotification(proposal.getProposedBy(), "Training proposal " + status.toLowerCase() + ": " + proposal.getTitle());
        if ("APPROVED".equals(status)) {
            // Convert approved proposal to a training
            TrainingDTO trainingDTO = new TrainingDTO();
            trainingDTO.setTitle(proposal.getTitle());
            trainingDTO.setDescription(proposal.getDescription());
            trainingDTO.setCategory(proposal.getCategory());
            trainingDTO.setCapacity(50); // Default capacity, can be adjusted
            createTraining(trainingDTO, token);
        }
        return toProposalDTO(proposal);
    }

    public List<TrainingProposalDTO> getMyProposals(String email) {
        return trainingProposalRepository.findByProposedBy(email)
                .stream()
                .map(this::toProposalDTO)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<TrainingProposalDTO> getPendingProposals() {
        return trainingProposalRepository.findByStatus("PENDING")
                .stream()
                .map(this::toProposalDTO)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private TrainingProposalDTO toProposalDTO(TrainingProposal proposal) {
        TrainingProposalDTO dto = new TrainingProposalDTO();
        dto.setId(proposal.getId());
        dto.setTitle(proposal.getTitle());
        dto.setDescription(proposal.getDescription());
        dto.setCategory(proposal.getCategory());
        dto.setProposedBy(proposal.getProposedBy());
        dto.setStatus(proposal.getStatus());
        dto.setCreatedAt(proposal.getCreatedAt());
        dto.setUpdatedAt(proposal.getUpdatedAt());
        dto.setHostCompany(proposal.getHostCompany());
        dto.setType(proposal.getType());
        return dto;
    }

    private TrainingProposal toProposalEntity(TrainingProposalDTO dto) {
        TrainingProposal proposal = new TrainingProposal();
        proposal.setTitle(dto.getTitle());
        proposal.setDescription(dto.getDescription());
        proposal.setCategory(dto.getCategory());
        proposal.setHostCompany(dto.getHostCompany());
        proposal.setType(dto.getType());
        return proposal;
    }

    private void updateProposalEntity(TrainingProposal proposal, TrainingProposalDTO dto) {
        proposal.setTitle(dto.getTitle());
        proposal.setDescription(dto.getDescription());
        proposal.setCategory(dto.getCategory());
        proposal.setHostCompany(dto.getHostCompany());
        proposal.setType(dto.getType());
    }
}