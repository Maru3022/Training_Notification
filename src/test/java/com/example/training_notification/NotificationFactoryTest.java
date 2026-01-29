package com.example.training_notification;


import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.factory.NotificationFactory;
import com.example.training_notification.service.interfaces.NotificationSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationFactoryTest {

    @Mock
    private NotificationSender emailSender;

    @InjectMocks
    private NotificationFactory notificationFactory;

    @Test
    @DisplayName("Should find  correct sender for EMAIL type")
    void getSender_EmailSuccess(){
        System.out.println("Testing Factory: searching for EMAIL sender");

        when(emailSender.supports(NotificationType.EMAIL))
                .thenReturn(true);

        ReflectionTestUtils.setField(notificationFactory, "senders", List.of(emailSender));
        NotificationSender result = notificationFactory.getSender(NotificationType.EMAIL);

        assertNotNull(result, "Resulting sender should not be null");
        System.out.println("SUCCESS: Found sender " + result.getClass().getSimpleName());
        assertEquals(emailSender,result);

    }
}
