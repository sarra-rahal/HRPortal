package com.HR.backend.service;

import com.HR.backend.dto.SignUpRequest;
import com.HR.backend.dto.SignUpResponse;
import com.HR.backend.model.User;
import com.HR.backend.repository.EmployeeRepository;
import com.HR.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public SignUpResponse signUp(SignUpRequest request) {
        logger.info("Processing signup for CIN: {}", request.getCin());

        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Passwords do not match for CIN: {}", request.getCin());
            return SignUpResponse.error("Passwords do not match", "PASSWORD_MISMATCH");
        }

        // Check if CIN exists in employee table
        if (!employeeRepository.existsByCin(request.getCin())) {
            logger.warn("CIN {} not found in employee table", request.getCin());
            return SignUpResponse.error("CIN not assigned to an employee", "CIN_NOT_FOUND");
        }

        // Check if CIN or email already exists in users table
        if (userRepository.existsByCin(request.getCin())) {
            logger.warn("Account already exists for CIN: {}", request.getCin());
            return SignUpResponse.error("Account already exists for this CIN", "CIN_EXISTS");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Email {} already in use", request.getEmail());
            return SignUpResponse.error("Email already in use", "EMAIL_EXISTS");
        }

        // Create new user
        User user = new User();
        user.setCin(request.getCin());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        logger.info("User created successfully for CIN: {}", request.getCin());
        return SignUpResponse.success("Account created successfully. Please check your email for confirmation.");

    }
}
