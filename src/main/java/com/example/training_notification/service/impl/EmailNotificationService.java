package com.example.training_notification.service.impl;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.service.interfaces.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationService implements NotificationSender {

    private final JavaMailSender mailSender;
    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(
            NotificationRequest request
    ){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("pdijsj2992@gmail.com");
            message.setTo(request.recipient());
            message.setSubject("Training Notification System");
            message.setText(request.message());

            mailSender.send(message);

            log.info(">>> SUCCESS: Email sent to {}", request.recipient());
            System.out.println(">>> [DEBUG] Notification sent via Email");
        }catch(Exception e){
            log.error(">>> ERROR: Failed to send email to {}. Error: {}",
                    request.recipient(), e.getMessage());
        }
    }

    @Override
    public boolean supports(
            NotificationType type
    ) {
        return type == NotificationType.EMAIL;
    }
}
