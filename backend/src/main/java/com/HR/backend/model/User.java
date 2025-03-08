package com.HR.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @Column(name = "cin")
    private String cin;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created")
    private LocalTime createD = LocalTime.now();

    @Column(name = "updated")
    private LocalTime updateD = LocalTime.now();
}