package com.HR.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "leave_balance")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
public abstract class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_cin", nullable = false)
    private String employeeCin;
}