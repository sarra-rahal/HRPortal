package com.HR.backend.controller;

import com.HR.backend.model.dto.TrainingDTO;
import com.HR.backend.model.dto.TrainingEnrollmentDTO;
import com.HR.backend.model.dto.TrainingProposalDTO;
import com.HR.backend.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {
    private final TrainingService trainingService;

    @GetMapping("/all")
    public ResponseEntity<List<TrainingDTO>> getAllTrainings() {
        return ResponseEntity.ok(trainingService.getAllTrainings());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<TrainingDTO> createTraining(@RequestBody TrainingDTO trainingDTO, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.createTraining(trainingDTO, token));
    }

    @PutMapping("/update/{trainingId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<TrainingDTO> updateTraining(@PathVariable Long trainingId, @RequestBody TrainingDTO trainingDTO, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.updateTraining(trainingId, trainingDTO, token));
    }

    @DeleteMapping("/delete/{trainingId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Void> deleteTraining(@PathVariable Long trainingId, @RequestHeader("Authorization") String token) {
        trainingService.deleteTraining(trainingId, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enroll/{trainingId}")
    public ResponseEntity<TrainingEnrollmentDTO> enroll(@PathVariable Long trainingId,
                                                        @RequestHeader("Authorization") String token) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(trainingService.enroll(trainingId, email, token));
    }

    @DeleteMapping("/cancel/{enrollmentId}")
    public ResponseEntity<Void> cancelEnrollment(@PathVariable Long enrollmentId) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        trainingService.cancelEnrollment(enrollmentId, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/review/{enrollmentId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<TrainingEnrollmentDTO> reviewEnrollment(@PathVariable Long enrollmentId,
                                                                  @RequestParam String status,
                                                                  @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.reviewEnrollment(enrollmentId, status, token));
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<List<TrainingEnrollmentDTO>> getMyEnrollments() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(trainingService.getEmployeeEnrollments(email));
    }

    @GetMapping("/pending-enrollments")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<TrainingEnrollmentDTO>> getPendingEnrollments() {
        return ResponseEntity.ok(trainingService.getPendingEnrollments());
    }

    @PostMapping("/propose")
    public ResponseEntity<TrainingProposalDTO> proposeTraining(@RequestBody TrainingProposalDTO proposalDTO, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.proposeTraining(proposalDTO, token));
    }

    @PutMapping("/proposal/update/{proposalId}")
    public ResponseEntity<TrainingProposalDTO> updateTrainingProposal(@PathVariable Long proposalId, @RequestBody TrainingProposalDTO proposalDTO, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.updateTrainingProposal(proposalId, proposalDTO, token));
    }

    @DeleteMapping("/proposal/cancel/{proposalId}")
    public ResponseEntity<Void> cancelTrainingProposal(@PathVariable Long proposalId, @RequestHeader("Authorization") String token) {
        trainingService.cancelTrainingProposal(proposalId, token);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/proposal/review/{proposalId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<TrainingProposalDTO> reviewTrainingProposal(@PathVariable Long proposalId, @RequestParam String status, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trainingService.reviewTrainingProposal(proposalId, status, token));
    }

    @GetMapping("/my-proposals")
    public ResponseEntity<List<TrainingProposalDTO>> getMyProposals() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(trainingService.getMyProposals(email));
    }

    @GetMapping("/pending-proposals")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<TrainingProposalDTO>> getPendingProposals() {
        return ResponseEntity.ok(trainingService.getPendingProposals());
    }
}
