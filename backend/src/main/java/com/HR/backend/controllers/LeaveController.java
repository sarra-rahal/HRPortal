package com.HR.backend.controllers;

import com.HR.backend.dtos.features.ReviewRequestDto;
import com.HR.backend.models.LeaveRequest;
import com.HR.backend.models.User;
import com.HR.backend.repositories.UserRepository;
import com.HR.backend.services.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;

    @PostMapping("/submit")
    public ResponseEntity<?> submitLeaveRequest(@RequestBody LeaveRequest request,
                                                @RequestHeader("Authorization") String token) {
        try {
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String cin = user.getCin();
            LeaveRequest savedRequest = leaveService.submitLeaveRequest(cin, request, token);
            return ResponseEntity.ok(savedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateLeaveRequest(@PathVariable Long id, @RequestBody LeaveRequest updatedRequest) {
        try {
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String cin = user.getCin();
            LeaveRequest updated = leaveService.updateLeaveRequest(id, cin, updatedRequest);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<LeaveRequest> cancelLeaveRequest(@PathVariable Long id) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String cin = user.getCin();
        return ResponseEntity.ok(leaveService.cancelLeaveRequest(id, cin));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<LeaveRequest>> getMyLeaveRequests() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String cin = user.getCin();
        return ResponseEntity.ok(leaveService.getEmployeeLeaveRequests(cin));
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Integer>> getLeaveBalance() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String cin = user.getCin();
        Map<String, Integer> balance = leaveService.getLeaveBalance(cin);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaveRequests() {
        return ResponseEntity.ok(leaveService.getPendingLeaveRequests());
    }

    @PutMapping("/review/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGER')")
    public ResponseEntity<LeaveRequest> reviewLeaveRequest(
            @PathVariable Long id,
            @RequestBody ReviewRequestDto reviewData) {
        return ResponseEntity.ok(leaveService.reviewLeaveRequest(id, reviewData.getStatus(), reviewData.getHrComment()));
    }

    @PutMapping("/note/{id}")
    public ResponseEntity<LeaveRequest> sendnote(
            @PathVariable Long id,
            @RequestBody String comment) {
        return ResponseEntity.ok(leaveService.sendnote(id, comment));
    }
}