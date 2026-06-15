package com.example.training_notification.saga;

import com.example.training_notification.dto.CompensationEvent;
import com.example.training_notification.dto.NotificationCommandEvent;
import com.example.training_notification.dto.NotificationResponseEvent;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.EmailNotificationService;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSagaConsumer {

    private final NotificationLogRepository notificationLogRepository;
    private final EmailNotificationService emailNotificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "saga.notification.send", containerFactory = "notificationKafkaListenerContainerFactory", groupId = "training-notification-saga")
    @Transactional
    public void handleSendNotification(NotificationCommandEvent event, Acknowledgment ack) {
        String correlationId = event.getCorrelationId();
        String userId = event.getUserId();
        log.info("Received saga.notification.send for correlationId={} userId={}", correlationId, userId);

        if (notificationLogRepository.existsByCorrelationId(correlationId)) {
            log.info("Idempotent skip: notification already processed correlationId={} userId={}", correlationId, userId);
            ack.acknowledge();
            return;
        }

        NotificationLog logEntry = new NotificationLog();
        logEntry.setCorrelationId(correlationId);
        try {
            boolean success = false;

            if (event.getEmail() != null && !event.getEmail().isBlank()) {
                // build a simple notification request and send synchronously
                com.example.training_notification.dto.NotificationRequest req = new com.example.training_notification.dto.NotificationRequest(
                        event.getEmail(),
                        "Hello, " + (event.getFirstName() != null ? event.getFirstName() : "user") + "!",
                        com.example.training_notification.dto.NotificationType.EMAIL
                );
                success = emailNotificationService.sendSync(req);
                logEntry.setEmail(event.getEmail());
                logEntry.setType(com.example.training_notification.dto.NotificationType.EMAIL);
            } else {
                // fallback: mark as PUSH and stub send
                log.info("No email provided, skipping send but logging as PUSH correlationId={} userId={}", correlationId, userId);
                logEntry.setType(com.example.training_notification.dto.NotificationType.PUSH);
                success = false;
            }

            logEntry.setUserId(parseUuid(userId));
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setStatus(NotificationLog.NotificationStatus.SENT);
            notificationLogRepository.save(logEntry);

            NotificationResponseEvent resp = new NotificationResponseEvent(correlationId, userId, success, null);
            kafkaTemplate.send("saga.notification.response", correlationId, resp);

            ack.acknowledge();
            log.info("Processed saga.notification.send completed correlationId={} userId={}", correlationId, userId);
        } catch (Exception e) {
            log.error("Error processing notification correlationId={} userId={}: {}", correlationId, userId, e.getMessage(), e);
            logEntry.setUserId(parseUuid(userId));
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setStatus(NotificationLog.NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            notificationLogRepository.save(logEntry);

            NotificationResponseEvent resp = new NotificationResponseEvent(correlationId, userId, false, e.getMessage());
            kafkaTemplate.send("saga.notification.response", correlationId, resp);

            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "saga.notification.compensate", containerFactory = "compensationKafkaListenerContainerFactory", groupId = "training-notification-saga")
    @Transactional
    public void handleCompensation(CompensationEvent event, Acknowledgment ack) {
        String correlationId = event.getCorrelationId();
        String userId = event.getUserId();
        log.info("Received saga.notification.compensate for correlationId={} userId={}", correlationId, userId);

        Optional<NotificationLog> maybe = notificationLogRepository.findByCorrelationId(correlationId);
        if (maybe.isPresent()) {
            NotificationLog entry = maybe.get();
            if (entry.getStatus() != NotificationLog.NotificationStatus.CANCELLED) {
                entry.setStatus(NotificationLog.NotificationStatus.CANCELLED);
                notificationLogRepository.save(entry);
                log.info("Marked notification CANCELLED correlationId={} userId={}", correlationId, userId);
            } else {
                log.info("Notification already CANCELLED correlationId={} userId={}", correlationId, userId);
            }
        } else {
            log.info("No notification log found to cancel correlationId={} userId={}", correlationId, userId);
        }

        ack.acknowledge();
    }

    private java.util.UUID parseUuid(String s) {
        try {
            return s == null ? null : java.util.UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }
}
