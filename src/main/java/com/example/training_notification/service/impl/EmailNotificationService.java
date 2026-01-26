package com.example.training_notification.service.impl;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmailNotificationService implements NotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    public EmailNotificationService(JavaMailSender mailSender, NotificationLogRepository notificationLogRepository) {
        this.mailSender = mailSender;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    @Async("taskExecutor")
    public void send(NotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("gravitya46@gmail.com");
            message.setTo(request.recipient());
            message.setSubject("Notification");
            message.setText(request.message());

            mailSender.send(message);

            log.info(">>> SUCCESS: Email sent to {}", request.recipient());
        } catch (Exception e) {
            log.error(">>> ERROR: Failed to send email to {}. Error: {}",
                    request.recipient(), e.getMessage());
        }
    }

    // Твой вспомогательный метод для обработки событий из Kafka
    public void processAndSentEmail(TrainingDTO training) {
        String messageContent = String.format("New workout plan assigned: %s scheduled for %s",
                training.training_name(), training.data());

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(training.userId());
        logEntry.setMessage(messageContent);
        logEntry.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(logEntry);
        log.info("Notification log saved for user: {}", training.userId());

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("gravitya46@gmail.com");
        mail.setTo("gravitya46@gmail.com");
        mail.setSubject("New Training Session");
        mail.setText(messageContent);

        try {
            mailSender.send(mail);
            log.info("Email successfully sent to admin");
        } catch (Exception e) {
            log.error("SMTP Error: {}", e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }
}