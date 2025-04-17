package com.HR.backend.repositories;

import com.HR.backend.models.TrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, Long> {
    List<TrainingEnrollment> findByEmployeeEmail(String employeeEmail);
    List<TrainingEnrollment> findByStatus(String status);
}