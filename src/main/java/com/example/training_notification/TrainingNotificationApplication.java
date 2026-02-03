package com.example.training_notification;

import jakarta.persistence.Cacheable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Cacheable
@EnableAsync
public class TrainingNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingNotificationApplication.class, args);
    }

}
