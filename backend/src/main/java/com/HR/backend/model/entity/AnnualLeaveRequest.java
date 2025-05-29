package com.HR.backend.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "annual_leave_requests")
@Data
public class AnnualLeaveRequest extends LeaveRequest {
}