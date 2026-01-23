package com.example.training_notification.dto;

public record NotificationRequest(
        String recipient,
        String message,
        NotificationType type
) {}