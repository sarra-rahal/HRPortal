package com.HR.backend.repository;

import com.HR.backend.model.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmail(String email);
    Optional<VerificationCode> findByCode(String code);
}