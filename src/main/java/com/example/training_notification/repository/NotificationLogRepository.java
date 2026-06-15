package com.example.training_notification.repository;

import com.example.training_notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    boolean existsBySagaIdAndStep(String sagaId, String step);

    boolean existsByCorrelationId(String correlationId);

    Optional<NotificationLog> findByCorrelationId(String correlationId);
}