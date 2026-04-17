package com.example.training_notification.listener;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.impl.TelegramNotificationService;
import com.example.training_notification.service.scheduler.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingListener {

    private final NotificationService notificationService;
    private final Optional<TelegramNotificationService> telegramService;

    @KafkaListener(
            topics = "training-events",
            groupId = "notification-clean-group"
    )
    public void listen(TrainingDTO trainingDTO) {
        log.info("Received new message from topic [training-events]: {}", trainingDTO);

        notificationService.processAndSendNotification(trainingDTO);
        String destination = trainingDTO.telegramTag();

        if (destination != null && !destination.isEmpty() && telegramService.isPresent()) {
            String messageText = String.format(
                    "Training update for %s%n%nTraining: %s%nTime: %s%nStatus: %s",
                    destination,
                    trainingDTO.training_name(),
                    trainingDTO.data(),
                    trainingDTO.status()
            );

            telegramService.get().sendTelegramMessage(destination, messageText);
        } else if (destination != null && !destination.isEmpty()) {
            log.debug("Skipping Telegram delivery: telegram.enabled=false or token is not configured");
        } else {
            log.warn("Skipping Telegram delivery: telegramTag is null or empty for user {}", trainingDTO.userId());
        }
    }
}
