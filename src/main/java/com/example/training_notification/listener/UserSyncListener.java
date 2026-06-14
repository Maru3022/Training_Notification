package com.example.training_notification.listener;

import com.example.training_notification.entity.User;
import com.example.training_notification.repository.UserRepository;
import com.example.training_notification.saga.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncListener {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user.created", containerFactory = "sagaKafkaListenerContainerFactory",
            groupId = "training-notification-saga")
    @Transactional
    public void onUserCreated(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        UUID userId = UUID.fromString(event.getUserId());
        User user = userRepository.findById(userId).orElseGet(User::new);
        user.setId(userId);
        user.setEmail(event.getEmail());
        user.setUsername(event.getUsername());
        user.setFullName(event.getFullName());
        userRepository.save(user);
        log.info("Synced user {} ({}) to local notification DB", userId, event.getEmail());
    }
}