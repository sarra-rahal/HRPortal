package com.HR.backend.model.dto;

import com.HR.backend.model.entity.Training.TrainingType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrainingDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int capacity;
    private int enrolledCount;
    private String category;
    private Double rating;
    private String hostCompany;
    private TrainingType type;
}