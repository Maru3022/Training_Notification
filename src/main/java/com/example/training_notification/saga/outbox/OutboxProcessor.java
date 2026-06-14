package com.example.training_notification.saga.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAt(OutboxEvent.Status.PENDING);
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload()).get();
                event.setStatus(OutboxEvent.Status.SENT);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error sending outbox event {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEvent.Status.FAILED);
                outboxEventRepository.save(event);
            }
        }
    }
}