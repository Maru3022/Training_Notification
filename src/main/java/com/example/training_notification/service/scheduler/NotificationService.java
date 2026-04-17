package com.example.training_notification.service.scheduler;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.factory.NotificationFactory;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for processing training notifications.
 * This is called by TrainingListener and InteractionListener which handle Kafka messages.
 * Note: The @KafkaListener annotation was removed to avoid duplicate processing.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationFactory notificationFactory;
    private final NotificationLogRepository notificationLogRepository;
    private final UserLookupService userLookupService;

    public void processAndSendNotification(TrainingDTO training) {
        String recipientEmail = userLookupService.getEmailByUserId(training.userId());
        String messageContent = "Workout '" + training.training_name() + "' status: " + training.status();

        NotificationRequest request = new NotificationRequest(
                recipientEmail, messageContent, NotificationType.EMAIL
        );

        notificationFactory.getSender(NotificationType.EMAIL).send(request);

        NotificationLog dbLog = new NotificationLog();
        dbLog.setUserId(training.userId());
        dbLog.setMessage(messageContent);
        dbLog.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(dbLog);
    }
}
