package com.example.training_notification.controller;

import com.example.training_notification.dto.TrainingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/test-send")
    public ResponseEntity<String> testNotification(
            @RequestBody TrainingDTO trainingDTO
    ) {
        kafkaTemplate.send("training-events", trainingDTO)
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
