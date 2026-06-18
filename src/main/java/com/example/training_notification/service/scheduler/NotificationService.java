package com.example.training_notification.service.scheduler;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.UserLookupService;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for processing training notifications.
 * This is called by TrainingListener and InteractionListener which handle Kafka messages.
 * Note: The @KafkaListener annotation was removed to avoid duplicate processing.
 */
@Service
@Slf4j
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final UserLookupService userLookupService;
    private final List<NotificationSender> availableSenders;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            UserLookupService userLookupService,
            Optional<List<NotificationSender>> availableSenders
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.userLookupService = userLookupService;
        this.availableSenders = availableSenders.orElseGet(Collections::emptyList);
    }

    public void processAndSendNotification(TrainingDTO training) {
        String messageContent = "Workout '" + training.training_name() + "' status: " + training.status();
        LocalDateTime parsedTrainingDate = parseTrainingDate(training.data());

        // Try to send via all available channels
        for (NotificationSender sender : availableSenders) {
            Optional<String> recipient = getRecipientForSender(sender, training);
            if (recipient.isPresent()) {
                NotificationRequest request = new NotificationRequest(
                        recipient.get(),
                        messageContent,
                        getTypeForSender(sender),
                        parsedTrainingDate,
                        training.status()
                );
                try {
                    sender.send(request);
                    log.info("Sent notification via {} to {}", getTypeForSender(sender), recipient.get());
                } catch (Exception e) {
                    log.error("Failed to send notification via {}: {}", getTypeForSender(sender), e.getMessage());
                }
            }
        }

        NotificationLog dbLog = new NotificationLog();
        dbLog.setUserId(training.userId());
        dbLog.setMessage(messageContent);
        dbLog.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(dbLog);
    }

    private Optional<String> getRecipientForSender(NotificationSender sender, TrainingDTO training) {
        if (sender.supports(NotificationType.EMAIL)) {
            String email = training.email() != null ? training.email() : userLookupService.getEmailByUserId(training.userId());
            return Optional.ofNullable(email);
        } else if (sender.supports(NotificationType.TELEGRAM)) {
            return Optional.ofNullable(training.telegramTag());
        } else if (sender.supports(NotificationType.PUSH)) {
            // For PUSH, use userId as recipient (as in existing send method)
            return Optional.of(training.userId().toString());
        }
        return Optional.empty();
    }

    private NotificationType getTypeForSender(NotificationSender sender) {
        for (NotificationType type : NotificationType.values()) {
            if (sender.supports(type)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type for sender");
    }

    private LocalDateTime parseTrainingDate(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(data);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse training date: {}", data);
            return null;
        }
    }
}
