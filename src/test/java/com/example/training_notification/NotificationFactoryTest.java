package com.example.training_notification;


import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.factory.NotificationFactory;
import com.example.training_notification.service.interfaces.NotificationSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationFactoryTest {

    @Mock
    private NotificationFactory notificationFactory;

    @Test
    @DisplayName("Should find  correct sender for EMAIL type")
    void getSender_EmailSuccess(){
        System.out.println("Testing Factory: searching for EMAIL sender");

        NotificationSender mockEmailSender = mock(NotificationSender.class);
        when(mockEmailSender.supports(NotificationType.EMAIL))
                .thenReturn(true);

        List<NotificationSender> senders = new ArrayList<>();
        senders.add(mockEmailSender);

        ReflectionTestUtils.setField(notificationFactory, "senders", senders);
        NotificationSender result = notificationFactory.getSender(NotificationType.EMAIL);

        System.out.println("Factory returned: " + result.getClass().getSimpleName());
        assertNotNull(result);
        assertTrue(result.supports(NotificationType.EMAIL));

    }
}
