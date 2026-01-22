package com.example.training_notification.service.interfaces;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;

public interface NotificationSender {
    void send(NotificationRequest request);
        boolean supports(NotificationType type);

}
