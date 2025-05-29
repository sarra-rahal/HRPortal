package com.HR.backend.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeDTO {
    private String cin;
    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String maritalStatus;
    private String email;
    private String phonenumber;
    private String address;
    private LocalDate hiredate;
    private String employeeStatus;
    private String employmentType;
    private Long departmentId;
    private String jobtitle;
    private BigDecimal salary;
    private String managercin;
    private List<String> certifications;
    private Integer personalLeaveBalance;
}