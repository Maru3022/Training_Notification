package com.example.training_notification.controller;

import com.example.training_notification.dto.TrainingDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationControllerTest {
    @Test void ignoresClientSuppliedRecipients() {
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), any())).thenReturn(new CompletableFuture<>());
        var controller = new NotificationController(kafka);
        ReflectionTestUtils.setField(controller, "testEmail", "server@example.test");
        ReflectionTestUtils.setField(controller, "testTelegramTag", "server-tag");
        controller.testNotification(new TrainingDTO(UUID.randomUUID(), "attacker-tag", "test", "now", "PLANNED", "victim@example.test", List.of()));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("training-events"), event.capture());
        TrainingDTO sent = (TrainingDTO) event.getValue();
        assertEquals("server@example.test", sent.email());
        assertEquals("server-tag", sent.telegramTag());
    }
}
