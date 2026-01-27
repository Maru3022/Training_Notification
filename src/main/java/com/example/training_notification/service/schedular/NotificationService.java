package com.example.training_notification.service.schedular;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.UserLookupService;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender emailNotificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final UserLookupService userLookupService;

    @KafkaListener(topics = "training-events", groupId = "notification-clean-v5")
    public void processAndSendNotification(TrainingDTO training) {
        try {
            // Вся тяжелая логика вынесена из HTTP потока сюда
            String recipientEmail = userLookupService.getEmailByUserId(training.userId());
            String messageContent = "Workout '" + training.training_name() + "' status: " + training.status();

            NotificationRequest request = new NotificationRequest(
                    recipientEmail, messageContent, NotificationType.EMAIL
            );

            emailNotificationService.send(request);

            NotificationLog dbLog = new NotificationLog();
            dbLog.setUserId(training.userId());
            dbLog.setMessage(messageContent);
            dbLog.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(dbLog);

        } catch (Exception e) {
            log.error("Worker error: {}", e.getMessage());
        }
    }
}