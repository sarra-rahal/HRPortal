package com.HR.backend.model.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String newPassword;
}