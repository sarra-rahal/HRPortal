package com.HR.backend.controller;

import com.HR.backend.model.dto.DocumentDTO;
import com.HR.backend.model.dto.DocumentRequestDTO;
import com.HR.backend.service.DocumentService;
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
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("access") String access,
            @RequestHeader("Authorization") String token) {
        DocumentDTO document = documentService.upload(file, access, token);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DocumentDTO>> getAllDocuments(@RequestHeader("Authorization") String token) {
        List<DocumentDTO> documents = documentService.getAccessibleDocuments(token);
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
    public ResponseEntity<DocumentRequestDTO> requestDocument(
            @RequestBody DocumentRequestDTO request,
            @RequestHeader("Authorization") String token) {
        DocumentRequestDTO savedRequest = documentService.requestDocument(request, token);
        return ResponseEntity.ok(savedRequest);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<DocumentRequestDTO>> getMyRequests(@RequestHeader("Authorization") String token) {
        List<DocumentRequestDTO> requests = documentService.getMyRequests(token);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending-requests")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<DocumentRequestDTO>> getPendingRequests(@RequestHeader("Authorization") String token) {
        List<DocumentRequestDTO> requests = documentService.getPendingRequests(token);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/review-request/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<DocumentRequestDTO> reviewRequest(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader("Authorization") String token) {
        DocumentRequestDTO updatedRequest = documentService.reviewRequest(id, status, token);
        return ResponseEntity.ok(updatedRequest);
    }

    @DeleteMapping("/cancel-request/{id}")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        documentService.cancelRequest(id, token);
        return ResponseEntity.ok().build();
    }

    /*
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<DocumentDTO> generateDocument(
            @RequestParam("name") String name,
            @RequestParam("parameters") String parametersJson,
            @RequestParam("accessibleBy") String accessibleBy,
            @RequestParam("templateType") String templateType,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> parameters = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(parametersJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            DocumentDTO document = documentService.generateDocument(name, parameters, accessibleBy, templateType, token);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    */
}