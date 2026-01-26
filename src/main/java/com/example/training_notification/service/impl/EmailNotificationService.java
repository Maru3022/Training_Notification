package com.example.training_notification.service.impl;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.service.interfaces.NotificationSender;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    @Async
    public void send(NotificationRequest request) {
        try {
            log.info("Starting async email send to {}", request.recipient());

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(request.recipient());
            helper.setSubject("Workout notification");

            Context context = new Context();

            context.setVariable("trainingName", request.message());
            context.setVariable("trainingDate", LocalDateTime.now().toString());
            context.setVariable("trainingStatus", "RECEIVED");

            String htmlContent = templateEngine.process("training-notification", context);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

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