package com.example.training_notification.listener;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.impl.PushNotificationService;
import com.example.training_notification.service.impl.TelegramNotificationService;
import com.example.training_notification.service.scheduler.NotificationService;
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
    private final TelegramNotificationService telegramService;

    @KafkaListener(
            topics = "training-events",
            groupId = "notification-clean-group"
    )
    public void listen(TrainingDTO trainingDTO) {
        log.info("Received new message from topic [training-events]: {}", trainingDTO);

        try {
            notificationService.processAndSendNotification(trainingDTO);
            String destination = trainingDTO.telegramTag();

            if (destination != null && !destination.isEmpty()) {
                String messageText = String.format(
                        "🏋️‍♂️ *Уведомление для %s*\n\n" +
                                "Тренировка: %s\n" +
                                "Время: %s\n" +
                                "Статус: %s",
                        destination,
                        trainingDTO.training_name(),
                        trainingDTO.data(),
                        trainingDTO.status()
                );

                telegramService.sendTelegramMessage(destination, messageText);
            } else {
                log.warn("Skipping Telegram delivery: telegramTag is null or empty for user {}", trainingDTO.userId());
            }
        } catch (Exception e) {
            log.error("Critical failure while processing Kafka message: {}", e.getMessage(), e);
        }
    }

}
