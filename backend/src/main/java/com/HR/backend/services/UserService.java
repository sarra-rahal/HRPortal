package com.HR.backend.services;

import com.HR.backend.models.Employee;
import com.HR.backend.models.User;
import com.HR.backend.repositories.EmployeeRepository;
import com.HR.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public Employee getProfile() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info("Fetching profile for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", email);
                    return new IllegalArgumentException("User not found");
                });
        Employee employee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> {
                    logger.error("Employee not found for CIN: {}", user.getCin());
                    return new IllegalArgumentException("Employee not found");
                });
        return employee;
    }

    public Employee updateProfile(Employee updatedEmployee) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info("Updating profile for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", email);
                    return new IllegalArgumentException("User not found");
                });

        Employee existingEmployee = employeeRepository.findByCin(user.getCin())
                .orElseThrow(() -> {
                    logger.error("Employee not found for CIN: {}", user.getCin());
                    return new IllegalArgumentException("Employee not found");
                });

        logger.debug("Updating employee with CIN: {} with data: {}", existingEmployee.getCin(), updatedEmployee);

        // Update allowed fields
        existingEmployee.setFirstname(updatedEmployee.getFirstname());
        existingEmployee.setLastname(updatedEmployee.getLastname());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setPhonenumber(updatedEmployee.getPhonenumber());
        existingEmployee.setAddress(updatedEmployee.getAddress());
        existingEmployee.setCertifications(updatedEmployee.getCertifications());

        // Update user email if changed
        if (!user.getEmail().equals(updatedEmployee.getEmail())) {
            logger.info("Updating user email from {} to {}", user.getEmail(), updatedEmployee.getEmail());
            user.setEmail(updatedEmployee.getEmail());
            userRepository.save(user);
        }

        Employee savedEmployee = employeeRepository.save(existingEmployee);
        logger.info("Profile updated successfully for CIN: {}", savedEmployee.getCin());
        return savedEmployee;
    }
}