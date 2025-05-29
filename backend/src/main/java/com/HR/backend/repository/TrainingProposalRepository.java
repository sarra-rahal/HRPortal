package com.HR.backend.repository;

import com.HR.backend.model.entity.TrainingProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingProposalRepository extends JpaRepository<TrainingProposal, Long> {
    List<TrainingProposal> findByProposedBy(String proposedBy);
    List<TrainingProposal> findByStatus(String status);
}