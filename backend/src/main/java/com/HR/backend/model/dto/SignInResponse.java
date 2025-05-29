package com.HR.backend.model.dto;

import lombok.Data;

@Data
public class SignInResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String firstname;
    private String lastname;
    private String email;
    private String errorCode;

    public static SignInResponse success(String message, String token, String role, String firstname, String lastname, String email) {
        SignInResponse response = new SignInResponse();
        response.success = true;
        response.message = message;
        response.token = token;
        response.role = role;
        response.firstname = firstname;
        response.lastname = lastname;
        response.email = email;
        return response;
    }

    public static SignInResponse error(String message, String errorCode) {
        SignInResponse response = new SignInResponse();
        response.success = false;
        response.message = message;
        response.errorCode = errorCode;
        return response;
    }
}