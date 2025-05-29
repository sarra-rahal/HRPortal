package com.HR.backend.model.dto;

import lombok.Data;

@Data
public class SignUpResponse {
    private boolean success;
    private String message;
    private String errorCode;

    public SignUpResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static SignUpResponse success(String message) {
        return new SignUpResponse(true, message, null);
    }

    public static SignUpResponse error(String message, String errorCode) {
        return new SignUpResponse(false, message, errorCode);
    }
}