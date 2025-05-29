package com.HR.backend.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveRequestDTO {
    private Long id;
    private String employeeCin;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private String reason;
    private String status;
    private String hrComment;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private String certificate;
}