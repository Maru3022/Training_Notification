package com.example.training_notification.service.impl;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.interfaces.NotificationSender;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class PushNotificationService implements NotificationSender {

    private final NotificationLogRepository notificationLogRepository;

    public void sendTrainingsNotification(TrainingDTO training) {
        String message = String.format("New workout plan assigned: %s scheduled for %s", training.training_name(), training.data());

        log.debug("Preparing to send notification for user ID: {}", training.userId());

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(training.userId());
        logEntry.setMessage(message);
        logEntry.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(logEntry);
        log.info("Notification log saved to database for user ID: {}", training.userId());
    }

    @Override
    public void send(NotificationRequest request) {
        log.info("Preparing to send push notification to {}", request.recipient());

        try {
            Notification notification = Notification.builder()
                    .setTitle("Workout notification")
                    .setBody(request.message())
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .setTopic(request.recipient())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully: {}", response);

            NotificationLog logEntry = new NotificationLog();
            logEntry.setUserId(null);
            logEntry.setMessage(request.message());
            logEntry.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to send push notification to {}: {}", request.recipient(), e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }
}
