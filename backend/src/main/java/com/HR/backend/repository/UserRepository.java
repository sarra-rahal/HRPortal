package com.HR.backend.repository;

import com.HR.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByCin(String cin);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}