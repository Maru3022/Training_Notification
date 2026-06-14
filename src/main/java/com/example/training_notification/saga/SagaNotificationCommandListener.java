package com.example.training_notification.saga;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.saga.outbox.OutboxEvent;
import com.example.training_notification.saga.outbox.OutboxEventRepository;
import com.example.training_notification.service.impl.EmailNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaNotificationCommandListener {

    private static final String STEP = "NOTIFICATION";

    private final EmailNotificationService emailNotificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "saga-notification-command",
            containerFactory = "sagaKafkaListenerContainerFactory",
            groupId = "training-notification-saga"
    )
    @Transactional
    public void onCommand(SagaCommandEvent event) {
        log.info("Received saga-notification-command: sagaId={}, status={}",
                event.getSagaId(), event.getStatus());

        if ("ROLLBACK".equals(event.getStatus())) {
            handleRollback(event);
            return;
        }

        if (!"EXECUTE".equals(event.getStatus())) {
            return;
        }

        if (notificationLogRepository.existsBySagaIdAndStep(event.getSagaId(), STEP)) {
            log.info("Notification for saga {} already processed, replying SUCCESS", event.getSagaId());
            publishResponse(event, "SUCCESS", null);
            return;
        }

        Map<String, Object> data = event.getData();
        String email = data != null ? String.valueOf(data.get("email")) : null;
        String username = data != null ? String.valueOf(data.get("username")) : "пользователь";
        String userId = data != null ? String.valueOf(data.get("userId")) : null;

        if (email == null || email.isBlank() || "null".equals(email)
                || userId == null || "null".equals(userId)) {
            publishResponse(event, "FAILED",
                    Map.of("reason", "email or userId missing in payload"));
            return;
        }

        NotificationRequest request = new NotificationRequest(
                email,
                "Добро пожаловать, " + username + "! Ваш аккаунт создан.",
                NotificationType.EMAIL
        );

        boolean sent = emailNotificationService.sendSync(request);

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(UUID.fromString(userId));
        logEntry.setMessage(request.message());
        logEntry.setSentAt(LocalDateTime.now());
        logEntry.setSagaId(event.getSagaId());
        logEntry.setStep(STEP);
        notificationLogRepository.save(logEntry);

        if (sent) {
            publishResponse(event, "SUCCESS", null);
        } else {
            publishResponse(event, "FAILED", Map.of("reason", "SMTP send failed"));
        }
    }

    private void handleRollback(SagaCommandEvent event) {
        Map<String, Object> data = event.getData();
        String email = data != null ? String.valueOf(data.get("email")) : null;
        if (email != null && !email.isBlank() && !"null".equals(email)) {
            NotificationRequest cancelRequest = new NotificationRequest(
                    email,
                    "Регистрация не завершена. Попробуйте зарегистрироваться позже.",
                    NotificationType.EMAIL
            );
            emailNotificationService.sendSync(cancelRequest);
        }
        publishResponse(event, "ROLLBACK_DONE", null);
    }

    private void publishResponse(SagaCommandEvent command, String status,
                                 Map<String, Object> extraData) {
        try {
            Map<String, Object> data = extraData != null ? new HashMap<>(extraData) : null;

            SagaResponseEvent response = new SagaResponseEvent();
            response.setEventId(UUID.randomUUID().toString());
            response.setSagaId(command.getSagaId());
            response.setStep(STEP);
            response.setStatus(status);
            response.setData(data);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setTopic("saga-notification-response");
            outboxEvent.setKey(command.getSagaId());
            outboxEvent.setPayload(objectMapper.writeValueAsString(response));
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to publish saga-notification-response: {}", e.getMessage(), e);
        }
    }
}