package com.example.training_notification.saga;

import lombok.Data;

@Data
public class UserCreatedEvent {
    private String eventId;
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private long occurredAt;
}