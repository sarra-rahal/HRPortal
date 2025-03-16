package com.HR.backend.dtos.auth;

import lombok.Data;

@Data
public class SignInRequest {
    private String email;
    private String password;
}