package com.HR.backend.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String userEmail;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
}