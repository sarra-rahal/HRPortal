package com.HR.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Data
public class Employee {
    @Id
    @Column(name = "cin")
    private String cin;

    @Column(name = "firstname", nullable = false)
    private String firstname;

    @Column(name = "lastname", nullable = false)
    private String lastname;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phonenumber")
    private String phonenumber;

    @Column(name = "hiredate", nullable = false)
    private LocalDate hiredate;

    @Column(name = "jobtitle", nullable = false)
    private String jobtitle;

    @Column(name = "departement", nullable = false)
    private String departement;

    @Column(name = "salary", nullable = false)
    private BigDecimal salary;

    @Column(name = "managernum")
    private String managernum;
}