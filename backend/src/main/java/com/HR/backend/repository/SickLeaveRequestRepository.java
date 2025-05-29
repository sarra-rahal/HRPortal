package com.HR.backend.repository;

import com.HR.backend.model.entity.SickLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SickLeaveRequestRepository extends JpaRepository<SickLeaveRequest, Long> {
}