package com.HR.backend.repository;

import com.HR.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByCin(String cin);
    boolean existsByEmail(String email);
}