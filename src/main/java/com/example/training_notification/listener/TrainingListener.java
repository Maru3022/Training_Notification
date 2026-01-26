package com.example.training_notification.listener;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.impl.PushNotificationService;
import com.example.training_notification.service.schedular.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingListener {

    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "training-events",
            groupId = "notification-clean-group"
    )
    public void listen(TrainingDTO trainingDTO) {
        log.info("Received new message from Kafka topic [training-events]:{}", trainingDTO);
        try {
            notificationService.processAndSendNotification(trainingDTO);
            log.info("Successfully processed training event for user: {}", trainingDTO.userId());
        } catch (Exception e) {
            log.error("Error processing Kafka message for user {}:{}", trainingDTO.userId(), e.getMessage());
        }
    }

}
