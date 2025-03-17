package com.HR.backend.dtos.auth;

import lombok.Data;

@Data
public class SignInResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String firstname;
    private String lastname;
    private String cin;
    private String errorCode;

    public static SignInResponse success(String message, String token, String role, String firstname, String lastname, String cin) {
        SignInResponse response = new SignInResponse();
        response.success = true;
        response.message = message;
        response.token = token;
        response.role = role;
        response.cin = cin;
        response.firstname = firstname;
        response.lastname = lastname;
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