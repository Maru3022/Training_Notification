package com.example.training_notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TrainingNotificationApplicationTests {

    @Test
    @DisplayName("Application main class should exist and be instantiable")
    void contextLoads() {
        assertDoesNotThrow(() -> {
            TrainingNotificationApplication application = new TrainingNotificationApplication();
            org.junit.jupiter.api.Assertions.assertNotNull(application);
        });
    }

}
