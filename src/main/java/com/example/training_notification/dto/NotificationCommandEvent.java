package com.example.training_notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCommandEvent {
    private String correlationId;
    private String userId;
    private String email;
    private String firstName;
}
