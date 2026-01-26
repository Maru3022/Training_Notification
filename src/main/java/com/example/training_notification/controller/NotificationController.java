package com.example.training_notification.controller;

import com.example.training_notification.dto.TrainingDTO;
import com.example.training_notification.service.schedular.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/test-send")
    public ResponseEntity<String> testNotification(
            @RequestBody TrainingDTO trainingDTO
    ) {
        log.info("Manual notification request received for user: {}", trainingDTO.userId());
        notificationService.sendTrainingNotification(trainingDTO);
        return ResponseEntity.ok("Notification processed successfully");
    }
}