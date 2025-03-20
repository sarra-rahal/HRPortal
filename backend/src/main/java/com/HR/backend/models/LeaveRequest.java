package com.HR.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_cin", nullable = false)
    private String employeeCin;

    @Column(name = "leave_type", nullable = false)
    private String leaveType; // e.g., "SICK", "VACATION", "PERSONAL"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "duration")
    private Integer duration; // in days

    @Column(name = "reason")
    private String reason;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // "PENDING", "APPROVED", "DENIED"

    @Column(name = "hr_comment")
    private String hrComment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}