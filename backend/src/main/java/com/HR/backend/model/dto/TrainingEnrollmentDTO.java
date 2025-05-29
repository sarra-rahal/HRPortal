package com.HR.backend.model.dto;

import lombok.Data;

@Data
public class TrainingEnrollmentDTO {
    private Long id;
    private TrainingDTO training;
    private String employeeEmail;
    private String status;
}