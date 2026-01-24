package com.example.training_notification.dto;

import java.util.List;
import java.util.UUID;

public record TrainingDTO(
        UUID userId,
        String training_name,
        String data,
        String status,
        List<Object> exercises
) {
}