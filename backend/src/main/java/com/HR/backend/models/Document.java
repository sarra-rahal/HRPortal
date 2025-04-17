package com.HR.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Column(nullable = false)
    private boolean generated;

    @Column(name = "accessible_by", nullable = false)
    private String accessibleBy;
}