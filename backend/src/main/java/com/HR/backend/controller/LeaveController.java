package com.HR.backend.controller;

import com.HR.backend.config.JwtUtil;
import com.HR.backend.model.dto.LeaveRequestDTO;
import com.HR.backend.model.dto.ReviewRequestDTO;
import com.HR.backend.service.LeaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);
    private final LeaveService leaveService;
    private final JwtUtil jwtUtil;

    @Data
    static class AnnualLeaveRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
    }

    private String extractEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid or missing Authorization header");
            throw new IllegalStateException("User not authenticated");
        }
        String jwt = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(jwt)) {
            logger.error("Invalid JWT token");
            throw new IllegalStateException("User not authenticated");
        }
        String email = jwtUtil.extractEmail(jwt);
        logger.debug("Extracted email: {}", email);
        return email;
    }

    @PostMapping("/submit/sick")
    public ResponseEntity<LeaveRequestDTO> submitSickLeaveRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam("certificate") MultipartFile certificate) {
        String email = extractEmailFromToken(authHeader);
        LeaveRequestDTO leaveRequest = leaveService.submitSickLeaveRequest(email, startDate, endDate, reason, certificate);
        return ResponseEntity.ok(leaveRequest);
    }

    @PostMapping("/submit/annual")
    public ResponseEntity<LeaveRequestDTO> submitAnnualLeaveRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AnnualLeaveRequest request) {
        String email = extractEmailFromToken(authHeader);
        LeaveRequestDTO leaveRequest = leaveService.submitAnnualLeaveRequest(
                email, request.getStartDate(), request.getEndDate(), request.getReason());
        return ResponseEntity.ok(leaveRequest);
    }

    @PutMapping("/update/sick/{id}")
    public ResponseEntity<LeaveRequestDTO> updateSickLeaveRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "certificate", required = false) MultipartFile certificate) {
        String email = extractEmailFromToken(authHeader);
        LeaveRequestDTO updatedRequest = leaveService.updateSickLeaveRequest(id, email, startDate, endDate, reason, certificate);
        return ResponseEntity.ok(updatedRequest);
    }

    @PutMapping("/update/annual/{id}")
    public ResponseEntity<LeaveRequestDTO> updateAnnualLeaveRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AnnualLeaveRequest request) {
        String email = extractEmailFromToken(authHeader);
        LeaveRequestDTO updatedRequest = leaveService.updateAnnualLeaveRequest(
                id, email, request.getStartDate(), request.getEndDate(), request.getReason());
        return ResponseEntity.ok(updatedRequest);
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelLeaveRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);
        leaveService.cancelLeaveRequest(id, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<LeaveRequestDTO>> getMyLeaveRequests(
            @RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);
        List<LeaveRequestDTO> requests = leaveService.getMyLeaveRequests(email);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingLeaveRequests() {
        logger.debug("Fetching all leave requests");
        List<LeaveRequestDTO> requests = leaveService.getPendingLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/review/{id}")
    public ResponseEntity<Void> reviewLeaveRequest(
            @PathVariable Long id,
            @RequestBody ReviewRequestDTO reviewRequest,
            @RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);
        leaveService.reviewLeaveRequest(id, reviewRequest.getStatus(), reviewRequest.getHrComment(), email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-note/{id}")
    public ResponseEntity<Void> sendNote(
            @PathVariable Long id,
            @RequestBody String note,
            @RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);
        leaveService.sendNote(id, note, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/balance")
    public ResponseEntity<Object> getLeaveBalance(@RequestHeader("Authorization") String authHeader) {
        String email = extractEmailFromToken(authHeader);
        Object balance = leaveService.getLeaveBalance(email);
        return ResponseEntity.ok(balance);
    }
}