package com.HR.backend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentRequestDTO {
    private Long id;
    private String type;
    private String description;
    private String status;
    private LocalDateTime requestDate;
    private boolean urgency;
    private String employeeEmail;
}