package com.example.training_notification.listener;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.schedular.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionListener {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "training-topic",
            groupId = "notification-clean-v4")
    public void listen(
            TrainingDTO trainingData
    ){
        log.info("Received Kafka message for user: {}", trainingData.userId());

        notificationService.processAndSendNotification(trainingData);
    }
}
