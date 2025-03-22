package com.HR.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "training_enrollments")
@Data
public class TrainingEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "training_id", nullable = false)
    private Training training;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(nullable = false)
    private String status;
}