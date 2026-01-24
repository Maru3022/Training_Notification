package com.example.training_notification.service.interfaces;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import org.springframework.scheduling.annotation.Async;

public interface NotificationSender {
    @Async("taskExecutor")
    void send(NotificationRequest request);

    boolean supports(NotificationType type);

}
