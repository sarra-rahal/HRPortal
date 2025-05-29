package com.HR.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false)
    private String gender = "Male";

    @Column(name = "nationality", nullable = false)
    private String nationality = "Tunisian";

    @Column(name = "marital_status", nullable = false)
    private String maritalStatus = "Single";

    @Column(name = "email", unique = true, nullable = false)
    private String email = "fatmaezzahra62@gmail.com";

    @Column(name = "phonenumber", nullable = false)
    private String phonenumber = "+21692353095";

    @Column(name = "address", nullable = false)
    private String address = "soukLahad-kebilli-4212";

    @Column(name = "hiredate", nullable = false)
    private LocalDate hiredate = LocalDate.of(2050, 3, 16);

    @Column(name = "employee_status", nullable = false)
    private String employeeStatus = "Active";

    @Column(name = "employment_type", nullable = false)
    private String employmentType = "Contractor";

    @ManyToOne
    @JoinColumn(name = "id_dep", nullable = false)
    private Department department;

    @Column(name = "jobtitle", nullable = false)
    private String jobtitle;

    @Column(name = "salary", nullable = false)
    private BigDecimal salary = new BigDecimal("800.00");

    @ManyToOne
    @JoinColumn(name = "managernum", referencedColumnName = "cin")
    @JsonBackReference
    private Employee managercin;

    @Column(name = "certifications", nullable = false)
    private String certifications = "";

    @Column(name = "personal_leave_balance", nullable = false)
    private Integer personalLeaveBalance = 20;

    public List<String> getCertificationsList() {
        return certifications.isEmpty() ? Arrays.asList() : Arrays.asList(certifications.split(","));
    }

    public void setCertificationsList(List<String> certifications) {
        this.certifications = certifications == null || certifications.isEmpty() ? "" : String.join(",", certifications);
    }
}