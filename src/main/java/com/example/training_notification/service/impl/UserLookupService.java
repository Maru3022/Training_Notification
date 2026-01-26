package com.example.training_notification.service.impl;

import com.example.training_notification.entity.User;
import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    @Cacheable(value = "userEmails", key = "#userId")
    public String getEmailByUserId(
            UUID userId
    ){
        log.info("Cache miss for user {}. Fetching from database...", userId);

        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
