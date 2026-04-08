package com.example.training_notification.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class MailConfig {
    // Mail sender is auto-configured by Spring Boot via application.properties
    // Caching is enabled here for centralized configuration
}
