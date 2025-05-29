package com.HR.backend.repository;

import com.HR.backend.model.entity.AnnualLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnualLeaveBalanceRepository extends JpaRepository<AnnualLeaveBalance, Long> {
    AnnualLeaveBalance findByEmployeeCin(String employeeCin);
}