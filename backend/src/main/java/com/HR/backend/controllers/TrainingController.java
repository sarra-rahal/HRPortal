package com.HR.backend.controllers;

import com.HR.backend.models.Training;
import com.HR.backend.models.TrainingEnrollment;
import com.HR.backend.services.TrainingService;
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
    public List<Training> getAllTrainings() {
        return trainingService.getAllTrainings();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public Training createTraining(@RequestBody Training training, @RequestHeader("Authorization") String token) {
        return trainingService.createTraining(training, token);
    }

    @PutMapping("/update/{trainingId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public Training updateTraining(@PathVariable Long trainingId, @RequestBody Training training, @RequestHeader("Authorization") String token) {
        training.setId(trainingId);
        return trainingService.updateTraining(training, token);
    }

    @DeleteMapping("/delete/{trainingId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Void> deleteTraining(@PathVariable Long trainingId, @RequestHeader("Authorization") String token) {
        trainingService.deleteTraining(trainingId, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enroll/{trainingId}")
    public TrainingEnrollment enroll(@PathVariable Long trainingId,
                                     @RequestHeader("Authorization") String token) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return trainingService.enroll(trainingId, email, token);
    }

    @DeleteMapping("/cancel/{enrollmentId}")
    public ResponseEntity<Void> cancelEnrollment(@PathVariable Long enrollmentId) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        trainingService.cancelEnrollment(enrollmentId, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/review/{enrollmentId}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public TrainingEnrollment reviewEnrollment(@PathVariable Long enrollmentId,
                                               @RequestParam String status,
                                               @RequestHeader("Authorization") String token) {
        return trainingService.reviewEnrollment(enrollmentId, status, token);
    }

    @GetMapping("/my-enrollments")
    public List<TrainingEnrollment> getMyEnrollments() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("Fetching enrollments for user: " + email);
        return trainingService.getEmployeeEnrollments(email);
    }

    @GetMapping("/pending-enrollments")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public List<TrainingEnrollment> getPendingEnrollments() {
        return trainingService.getPendingEnrollments();
    }
}