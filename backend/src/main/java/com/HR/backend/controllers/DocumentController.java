package com.HR.backend.controllers;

import com.HR.backend.models.Document;
import com.HR.backend.models.DocumentRequest;
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
    public ResponseEntity<Document> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("access") String access,
            @RequestHeader("Authorization") String token) {
        System.out.println("Upload - Token Received: " + token);
        Document document = documentService.upload(file, access, token);
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

    @PostMapping("/request")
    public ResponseEntity<DocumentRequest> requestDocument(
            @RequestBody DocumentRequest request,
            @RequestHeader("Authorization") String token) {
        DocumentRequest savedRequest = documentService.requestDocument(request, token);
        return ResponseEntity.ok(savedRequest);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<DocumentRequest>> getMyRequests(@RequestHeader("Authorization") String token) {
        List<DocumentRequest> requests = documentService.getMyRequests(token);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending-requests")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<DocumentRequest>> getPendingRequests(@RequestHeader("Authorization") String token) {
        List<DocumentRequest> requests = documentService.getPendingRequests(token);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/review-request/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<DocumentRequest> reviewRequest(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader("Authorization") String token) {
        DocumentRequest updatedRequest = documentService.reviewRequest(id, status, token);
        return ResponseEntity.ok(updatedRequest);
    }

    @DeleteMapping("/cancel-request/{id}")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        documentService.cancelRequest(id, token);
        return ResponseEntity.ok().build();
    }

    // Commented-out generate endpoint (unchanged)
    /*
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<Document> generateDocument(
            @RequestParam("name") String name,
            @RequestParam("parameters") String parametersJson,
            @RequestParam("accessibleBy") String accessibleBy,
            @RequestParam("templateType") String templateType,
            @RequestHeader("Authorization") String token) {
        System.out.println("Generate - Token Received: " + token);
        try {
            Map<String, Object> parameters = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(parametersJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            Document document = documentService.generateDocument(name, parameters, accessibleBy, templateType, token);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    */
}