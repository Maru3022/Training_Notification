package com.example.training_notification.service.impl;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;

    public void sendTrainingsNotification(
            TrainingDTO training
    ) {
        String message = String.format("New workout plan assigned: %s scheduled for %s", training.training_name(), training.data());

        log.debug("Preparing to send notification for user ID: {}", training.userId());

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(training.userId());
        logEntry.setMessage(message);
        logEntry.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(logEntry);
        log.info("Notification log saved to database for user ID: {}", training.userId());

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo("gravitya46@gmail.com");
            mail.setSubject("New Training Session Notification");
            mail.setText(message);
            mailSender.send(mail);
            log.info("Email notification successfully sent to: {}", mail.getTo()[0]);
        } catch (Exception e) {
            log.error("Failed to sent email notification: {}", e.getMessage());
        }
    }
}
