package com.example.training_notification;

import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.factory.NotificationFactory;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.UserLookupService;
import com.example.training_notification.service.interfaces.NotificationSender;
import com.example.training_notification.service.scheduler.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private NotificationSender emailNotificationService;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should process a training event and persist a notification log")
    void processAndSendNotification() {
        UUID userId = UUID.randomUUID();
        TrainingDTO training = new TrainingDTO(
                userId,
                "@test_user",
                "Crossfit",
                "2024-05-20",
                "COMPLETED",
                null,
                null
        );

        when(userLookupService.getEmailByUserId(userId)).thenReturn("user@fitness.com");
        when(notificationFactory.getSender(NotificationType.EMAIL)).thenReturn(emailNotificationService);

        notificationService.processAndSendNotification(training);

        verify(emailNotificationService, times(1)).send(any());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(userId, savedLog.getUserId());
        assertEquals("Workout 'Crossfit' status: COMPLETED", savedLog.getMessage());
    }
}
