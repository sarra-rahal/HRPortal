package com.HR.backend.model.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String employeeNumber;
    private String email;
}