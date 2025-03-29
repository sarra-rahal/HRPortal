package com.HR.backend.controllers;

import com.HR.backend.dtos.auth.SignInRequest;
import com.HR.backend.dtos.auth.SignInResponse;
import com.HR.backend.dtos.auth.SignUpRequest;
import com.HR.backend.dtos.auth.SignUpResponse;
import com.HR.backend.services.AuthService;
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
}