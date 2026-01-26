package com.example.training_notification.service.schedular;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.PushNotificationService;
import com.example.training_notification.service.impl.UserLookupService;
import com.example.training_notification.service.interfaces.NotificationSender;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender emailNotificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final UserLookupService userLookupService;

    private void saveLogToDatabase(
            UUID userId,
            String message
    ){
        NotificationLog dbLog = new NotificationLog();
        dbLog.setUserId(userId);
        dbLog.setMessage(message);
        dbLog.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(dbLog);
        log.info("Database log saved for user: {}",userId);
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
        saveLogToDatabase(null,"Manual email to " + email + ": " + content);
    }

    @Transactional
    public void processAndSendNotification(
            TrainingDTO training
    ) {
        try{
            log.info("Processing notification: User {} created workout '{}'",
                    training.userId(), training.training_name());

            String recipientEmail = userLookupService.getEmailByUserId(training.userId());

            String name = (training.training_name() != null) ? training.training_name() : "Workout";
            String status = (training.status() != null) ? training.status() : "REGISTERED";

            String messageContent = "Hello! A new workout '" + training.training_name() + "' has been created!" +
                    "' has been registered with status: " + training.status();

            NotificationRequest request = new NotificationRequest(
                    recipientEmail,
                    messageContent,
                    NotificationType.EMAIL
            );

            emailNotificationService.send(request);
            log.info("Notification successfully sent to provider for user: {}", training.userId());

            saveLogToDatabase(training.userId(),messageContent);

            log.info("Database log updated for user: {}", training.userId());
        }catch (Exception e){
            log.error("Error during notification processing for user {}: {}", training.userId(), e.getMessage());
        }

    }
}
