package com.HR.backend.controllers;


import com.HR.backend.models.Notification;
import com.HR.backend.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;

    @GetMapping("/my-notifications")
    public List<Notification> getMyNotifications(@RequestParam String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
    }
}