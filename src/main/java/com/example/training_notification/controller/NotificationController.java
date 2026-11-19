package com.example.training_notification.controller;

import com.example.training_notification.dto.TrainingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${notification.test-recipient.email}")
    private String testEmail;

    @Value("${notification.test-recipient.telegram-tag:}")
    private String testTelegramTag;

    @PostMapping("/test-send")
    public ResponseEntity<Void> testNotification(
            @RequestBody TrainingDTO trainingDTO
    ) {
        TrainingDTO serverAddressed = new TrainingDTO(trainingDTO.userId(), testTelegramTag,
                trainingDTO.training_name(), trainingDTO.data(), trainingDTO.status(), testEmail,
                trainingDTO.exercises());
        kafkaTemplate.send("training-events", serverAddressed)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed for user {}: {}", trainingDTO.userId(), ex.getMessage(), ex);
                    } else {
                        log.info("Kafka message sent to topic training-events, offset={}",
                                result.getRecordMetadata().offset());
                    }
                });
        return ResponseEntity.accepted().build();
    }
}
