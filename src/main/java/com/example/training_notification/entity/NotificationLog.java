package com.example.training_notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs", indexes = {@Index(name = "idx_notification_correlation", columnList = "correlation_id")})
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "correlation_id", unique = true)
    private String correlationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private com.example.training_notification.dto.NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "step")
    private String step;

    public enum NotificationStatus {
        SENT,
        FAILED,
        CANCELLED
    }
}