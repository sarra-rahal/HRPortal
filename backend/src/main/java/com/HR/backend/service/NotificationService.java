package com.HR.backend.service;

import com.HR.backend.model.dto.NotificationDTO;
import com.HR.backend.model.entity.Notification;
import com.HR.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<NotificationDTO> getMyNotifications(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    public static void sendNotification(String email, String message, NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        Notification notification = new Notification();
        notification.setUserEmail(email);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setUserEmail(notification.getUserEmail());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setCreatedAt(notification.getCreatedAt());
        notificationDTO.setRead(notification.isRead());

        messagingTemplate.convertAndSendToUser(email, "/topic/notifications", notificationDTO);
    }

    private NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserEmail(notification.getUserEmail());
        dto.setMessage(notification.getMessage());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRead(notification.isRead());
        return dto;
    }
}
