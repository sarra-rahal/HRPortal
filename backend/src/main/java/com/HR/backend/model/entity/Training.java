package com.HR.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "trainings")
@Data
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount = 0;

    @Column(nullable = false)
    private String category;

    @Column
    private Double rating;

    @Column(name = "host_company", nullable = false)
    private String hostCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingType type;

    public enum TrainingType {
        INTERNAL, EXTERNAL
    }
}