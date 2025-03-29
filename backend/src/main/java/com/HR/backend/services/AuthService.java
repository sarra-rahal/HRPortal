package com.HR.backend.services;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.dtos.auth.SignInRequest;
import com.HR.backend.dtos.auth.SignInResponse;
import com.HR.backend.dtos.auth.SignUpRequest;
import com.HR.backend.dtos.auth.SignUpResponse;
import com.HR.backend.models.Employee;
import com.HR.backend.models.User;
import com.HR.backend.models.VerificationCode;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.UserRepository;
import com.HR.backend.repositories.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public SignUpResponse signUp(SignUpRequest request) {
        logger.info("Processing signup for CIN: {}", request.getCin());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Passwords are not matching for the cin : {}", request.getCin());
            return SignUpResponse.error("Passwords do not match", "PASSWORD_MISMATCH");
        }

        Employee employee = employeeRepository.findByCin(request.getCin())
                .orElseThrow(() -> new IllegalArgumentException("No employee with this CIN"));

        if (userRepository.existsByCin(request.getCin())) {
            logger.warn("Account already exists for the employee with the CIN: {}", request.getCin());
            return SignUpResponse.error("Account already exists for the employee with this CIN", "CIN_EXISTS");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Email {} already in use", request.getEmail());
            return SignUpResponse.error("Already exists an account with this email", "EMAIL_EXISTS");
        }

        // Generate and send verification code
        String code = generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(request.getEmail());
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        verificationCodeRepository.save(verificationCode);

        // Send email with verification code
        String maskedEmail = maskEmail(request.getEmail());
        emailService.sendEmail(
                "noreply@hrportal.com",
                request.getEmail(),
                "Verify Your HR Portal Account",
                "Your verification code is: " + code + "\nThis code expires in 15 minutes."
        );

        return SignUpResponse.success("Verification code sent to " + maskedEmail + ". Please check your email.");
    }

    public SignUpResponse verifyCode(String email, String code, SignUpRequest request) {
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No verification code found for this email"));

        if (LocalDateTime.now().isAfter(verificationCode.getExpiresAt())) {
            verificationCodeRepository.delete(verificationCode);
            return SignUpResponse.error("Verification code expired", "CODE_EXPIRED");
        }

        if (!verificationCode.getCode().equals(code)) {
            return SignUpResponse.error("Invalid verification code", "INVALID_CODE");
        }

        // Code is valid, proceed with user creation
        Employee employee = employeeRepository.findByCin(request.getCin())
                .orElseThrow(() -> new IllegalArgumentException("CIN not found"));

        User user = new User();
        user.setCin(request.getCin());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("HR Manager".equalsIgnoreCase(employee.getJobtitle())
                ? User.UserRole.HR_MANAGER
                : User.UserRole.EMPLOYEE);

        userRepository.save(user);
        verificationCodeRepository.delete(verificationCode); // Clean up
        logger.info("User created with role: {}", user.getRole());
        return SignUpResponse.success("Account created successfully.");
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        return "*****" + localPart.charAt(localPart.length() - 1) + "@" + domain;
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