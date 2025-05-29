package com.HR.backend.model.dto;

import com.HR.backend.model.entity.TrainingProposal.TrainingType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrainingProposalDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String proposedBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String hostCompany;
    private TrainingType type;
}