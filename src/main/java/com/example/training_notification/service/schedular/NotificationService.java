package com.example.training_notification.service.schedular;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.impl.PushNotificationService;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationSender emailNotificationService;
    private final PushNotificationService pushNotificationService;

    @KafkaListener(
            topics = "training-events",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processNotification(
            TrainingDTO training
    ) {
        log.info("Notification: User {} has created a new workout: '{}'",
                training.userId(), training.training_name());

        NotificationRequest request = new NotificationRequest(
                "gravitya46@gmail.com",
                "You have created a new workout: " + training.training_name(),
                NotificationType.EMAIL
        );

        emailNotificationService.send(request);
    }

    public void sendTrainingNotification(TrainingDTO training) {
        log.info("Preparing notification for user: {}", training.userId());

        NotificationRequest request = new NotificationRequest(
                "gravitya46@gmail.com",
                "You have created a new workout: " + training.training_name(),
                NotificationType.EMAIL
        );

        emailNotificationService.send(request);
    }
}
