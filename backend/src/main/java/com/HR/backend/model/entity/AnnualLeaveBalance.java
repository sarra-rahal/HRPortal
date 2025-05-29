package com.HR.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "annual_leave_balance")
@Data
public class AnnualLeaveBalance extends LeaveBalance {
    @Column(name = "balance", nullable = false)
    private Integer balance;
}