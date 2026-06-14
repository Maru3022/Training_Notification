package com.example.training_notification.saga;

import lombok.Data;

import java.util.Map;

@Data
public class SagaCommandEvent {
    private String eventId;
    private String sagaId;
    private String step;
    private String status;
    private Map<String, Object> data;
}