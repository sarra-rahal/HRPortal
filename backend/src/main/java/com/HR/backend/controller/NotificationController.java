package com.HR.backend.controller;

import com.HR.backend.model.dto.NotificationDTO;
import com.HR.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/my-notifications")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(@RequestParam String email) {
        return ResponseEntity.ok(notificationService.getMyNotifications(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
}