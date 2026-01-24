package com.example.training_notification.factory;

import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.service.interfaces.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.notification.UnableToSendNotificationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationFactory {

    @Autowired
    private List<NotificationSender> senders;

    public NotificationSender getSender(NotificationType type) {
        return senders.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new UnableToSendNotificationException("Sender not found for: " + type));
    }
}