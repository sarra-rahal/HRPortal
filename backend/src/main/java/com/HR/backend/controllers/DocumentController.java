// src/main/java/com/HR/backend/controllers/DocumentController.java
package com.HR.backend.controllers;

import com.HR.backend.models.Document;
import com.HR.backend.services.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accessibleBy") String accessibleBy,
            @RequestHeader("Authorization") String token) {
        System.out.println("Upload - Token Received: " + token);
        Document document = documentService.uploadDocument(file, accessibleBy, token);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Document> generateDocument(
            @RequestParam("name") String name,
            @RequestParam("content") String content,
            @RequestParam("accessibleBy") String accessibleBy,
            @RequestParam("templateType") String templateType,
            @RequestHeader("Authorization") String token) {
        System.out.println("Generate - Token Received: " + token);
        Document document = documentService.generateDocument(name, content, accessibleBy, templateType, token);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Document>> getAllDocuments(@RequestHeader("Authorization") String token) {
        List<Document> documents = documentService.getAccessibleDocuments(token);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Resource resource = documentService.downloadDocument(id, token);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}