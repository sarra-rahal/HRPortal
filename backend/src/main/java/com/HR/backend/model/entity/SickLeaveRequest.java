package com.HR.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sick_leave_requests")
@Data
public class SickLeaveRequest extends LeaveRequest {
    @Column(name = "certificate")
    private byte[] certificate;
}