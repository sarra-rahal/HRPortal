package com.HR.backend.repository;

import com.HR.backend.model.entity.TrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, Long> {
    List<TrainingEnrollment> findByEmployeeEmail(String employeeEmail);
    List<TrainingEnrollment> findByStatus(String status);
    List<TrainingEnrollment> findByTrainingId(Long trainingId);
}