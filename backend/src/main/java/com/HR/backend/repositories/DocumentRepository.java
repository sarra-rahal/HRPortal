package com.HR.backend.repositories;

import com.HR.backend.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DocumentRepository extends JpaRepository<Document, Long> {

}