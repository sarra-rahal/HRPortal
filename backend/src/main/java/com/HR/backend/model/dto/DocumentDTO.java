package com.HR.backend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private String filePath;
    private String uploadedBy;
    private LocalDateTime uploadDate;
    private boolean generated;
    private List<String> accessibleBy;
}