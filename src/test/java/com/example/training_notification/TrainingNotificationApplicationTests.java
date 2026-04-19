package com.example.training_notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrainingNotificationApplicationTests {

    @Test
    @DisplayName("Application class should be constructible")
    void contextLoads() {
        assertDoesNotThrow(() -> {
            TrainingNotificationApplication application = new TrainingNotificationApplication();
            assertNotNull(application);
        });
    }
}
