package com.HR.backend.services;

import com.HR.backend.models.Notification;
import com.HR.backend.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    public void sendEmail(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void notifyUser(String email, String message) {
        Notification notification = new Notification();
        notification.setUserEmail(email);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }
}