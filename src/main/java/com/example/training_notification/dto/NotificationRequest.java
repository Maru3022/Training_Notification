package com.example.training_notification.dto;

import java.time.LocalDateTime;

public record NotificationRequest(
        String recipient,
        String message,
        NotificationType type,
        LocalDateTime trainingDate,
        String trainingStatus
) {
    public NotificationRequest(String recipient, String message, NotificationType type) {
        this(recipient, message, type, null, null);
    }
}