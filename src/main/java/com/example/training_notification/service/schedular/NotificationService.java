package com.example.training_notification.service.schedular;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.PushNotificationService;
import com.example.training_notification.service.interfaces.NotificationSender;
import jakarta.transaction.Transactional;
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
    private final PushNotificationService pushNotificationService;
    private final NotificationLogRepository notificationLogRepository;

    public void sendTrainingNotification(TrainingDTO training) {
        log.info("Preparing notification for user: {}", training.userId());

        NotificationRequest request = new NotificationRequest(
                "gravitya46@gmail.com",
                "You have created a new workout: " + training.training_name(),
                NotificationType.EMAIL
        );

        emailNotificationService.send(request);
    }

    public void sendManualNotification(
            String email,
            String subject,
            String content
    ){
        NotificationRequest request = new NotificationRequest(
                email,
                content,
                NotificationType.EMAIL
        );

        emailNotificationService.send(request);

        NotificationLog logEntry = new NotificationLog();
        logEntry.setMessage(content);
        logEntry.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(logEntry);
    }

    @Transactional
    public void processAndSendNotification(
            TrainingDTO training
    ) {
        try{
            log.info("Processing notification: User {} created workout '{}'",
                    training.userId(), training.training_name());

            String messageContent = String.format("Hello! New workout '%s' has been registered with status: %s",
                    training.training_name(), training.status());

            NotificationRequest request = new NotificationRequest(
                    "gravitya46@gmail.com",
                    messageContent,
                    NotificationType.EMAIL
            );

            emailNotificationService.send(request);
            log.info("Notification successfully sent to provider for user: {}", training.userId());

            NotificationLog dbLog = new NotificationLog();
            dbLog.setUserId(training.userId());
            dbLog.setMessage(messageContent);
            dbLog.setSentAt(LocalDateTime.now());
            notificationLogRepository.save(dbLog);

            log.info("Database log updated for user: {}", training.userId());
        }catch (Exception e){
            log.error("Error during notification processing for user {}: {}", training.userId(), e.getMessage());
        }

    }
}
