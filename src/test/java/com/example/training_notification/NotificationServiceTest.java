package com.example.training_notification;


import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.entity.NotificationLog;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.service.impl.UserLookupService;
import com.example.training_notification.service.interfaces.NotificationSender;
import com.example.training_notification.service.schedular.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private UserLookupService userLookupService ;

    @Mock
    private NotificationSender emailNotificationService;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should process training event and save log to database")
    void processAndSendNotification() {
        System.out.println("Starting complex process: Mapping user and saving notification log");

        UUID userId = UUID.randomUUID();
        TrainingDTO training =  new TrainingDTO(userId,"Crossfit", "2024-05-20","COMPLETED", null,null);

        when(userLookupService.getEmailByUserId(userId))
                .thenReturn("user@fitness.com");

        notificationService.processAndSendNotification(training);

        System.out.println("Verifying service interactions...");
        verify(emailNotificationService,times(1)).send(any());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        System.out.println("Log saved with message: "  + savedLog.getMessage());

        assertEquals(userId, savedLog.getUserId());
        assertEquals("Workout 'Crossfit' status: COMPLETED", savedLog.getMessage());


    }
}