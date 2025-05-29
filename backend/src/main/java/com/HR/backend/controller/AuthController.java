package com.HR.backend.controller;

import com.HR.backend.model.dto.SignInRequest;
import com.HR.backend.model.dto.SignInResponse;
import com.HR.backend.model.dto.SignUpRequest;
import com.HR.backend.model.dto.SignUpResponse;
import com.HR.backend.model.dto.ForgotPasswordRequest;
import com.HR.backend.model.dto.ResetPasswordRequest;
import com.HR.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<SignUpResponse> verifyCode(
            @RequestParam String email,
            @RequestParam String code,
            @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.verifyCode(email, code, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
        SignInResponse response = authService.signIn(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SignUpResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        SignUpResponse response = authService.forgotPassword(request.getEmployeeNumber(), request.getEmail());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SignUpResponse> resetPassword(
            @RequestParam String token,
            @RequestBody ResetPasswordRequest request) {
        SignUpResponse response = authService.resetPassword(token, request.getNewPassword());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}