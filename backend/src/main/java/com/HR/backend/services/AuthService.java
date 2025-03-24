package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.dtos.auth.SignInRequest;
import com.HR.backend.dtos.auth.SignInResponse;
import com.HR.backend.dtos.auth.SignUpRequest;
import com.HR.backend.dtos.auth.SignUpResponse;
import com.HR.backend.models.Employee;
import com.HR.backend.models.User;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SignUpResponse signUp(SignUpRequest request) {
        logger.info("Processing signup for CIN: {}", request.getCin());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Passwords do not match for CIN: {}", request.getCin());
            return SignUpResponse.error("Passwords do not match", "PASSWORD_MISMATCH");
        }

        Employee employee = employeeRepository.findByCin(request.getCin())
                .orElseThrow(() -> new IllegalArgumentException("CIN not found"));

        if (userRepository.existsByCin(request.getCin())) {
            logger.warn("Account already exists for CIN: {}", request.getCin());
            return SignUpResponse.error("Account already exists for this CIN", "CIN_EXISTS");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Email {} already in use", request.getEmail());
            return SignUpResponse.error("Email already in use", "EMAIL_EXISTS");
        }

        User user = new User();
        user.setCin(request.getCin());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("HR Manager".equalsIgnoreCase(employee.getJobtitle())
                ? User.UserRole.HR_MANAGER
                : User.UserRole.EMPLOYEE);

        userRepository.save(user);
        logger.info("User created with role: {}", user.getRole());
        return SignUpResponse.success("Account created successfully.");
    }

    public SignInResponse signIn(SignInRequest request) {
        logger.info("Processing sign-in for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Invalid password for email: {}", request.getEmail());
            return SignInResponse.error("Invalid email or password", "INVALID_CREDENTIALS");
        }

        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        logger.info("Sign-in successful for email: {}", request.getEmail());
        return SignInResponse.success(
                "Sign-in successful",
                token,
                user.getRole().name(),
                employee.getFirstname(),
                employee.getLastname(),
                user.getCin(),
                user.getEmail()
        );
    }
}