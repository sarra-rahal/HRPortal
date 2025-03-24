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
    @PreAuthorize("hasRole('HR_MANAGER')")
    public Training createTraining(@RequestBody Training training, @RequestHeader("Authorization") String token) {
        return trainingService.createTraining(training, token);
    }

    @PostMapping("/enroll/{trainingId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public TrainingEnrollment enroll(@PathVariable Long trainingId,
                                     @RequestHeader("Authorization") String token) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return trainingService.enroll(trainingId, email, token);
    }

    @PutMapping("/review/{enrollmentId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public TrainingEnrollment reviewEnrollment(@PathVariable Long enrollmentId,
                                               @RequestParam String status,
                                               @RequestHeader("Authorization") String token) {
        return trainingService.reviewEnrollment(enrollmentId, status, token);
    }

    @GetMapping("/my-enrollments")
    public List<TrainingEnrollment> getMyEnrollments(@RequestParam String employeeEmail) {
        return trainingService.getEmployeeEnrollments(employeeEmail);
    }
}