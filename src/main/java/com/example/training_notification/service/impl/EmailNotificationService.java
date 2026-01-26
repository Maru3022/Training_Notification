package com.example.training_notification.service.impl;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    @Async
    public void send(NotificationRequest request) {
        try {
            log.info("Starting async email send to {}", request.recipient());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(request.recipient());
            message.setSubject("Workout notification");
            message.setText(request.message());

            mailSender.send(message);

            log.info(">>> SUCCESS: Email sent to {}", request.recipient());
        } catch (Exception e) {
            log.error(">>> ERROR: Failed to send email to {}. Error: {}",
                    request.recipient(), e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }
}