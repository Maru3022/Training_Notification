package com.example.training_notification.service;

import com.example.training_notification.dto.TrainingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    @KafkaListener(
            topics = "training-events",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processNotification(TrainingDTO training){
        log.info("\uD83D\uDCE2 Notification: User {} has created a new workout: '{}'",
                training.userId(), training.training_name());

        performNotify(training);
    }

    private void performNotify(TrainingDTO dto){
        System.out.println(">>> [SEND] notification sent for status");
    }
}
