package com.example.training_notification.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true", matchIfMissing = true)
public class TelegramNotificationService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendTelegramMessage(String target, String text){
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.telegram.org/bot" + botToken + "/sendMessage")
                .queryParam("chat_id", target)
                .queryParam("text", text)
                .toUriString();

        try{
            restTemplate.getForObject(url,String.class);
            log.info("[SUCCESS] Telegram notification sent to: {}", target);
        }catch (Exception e){
            log.error("[ERROR] Failed to send Telegram notification to {}: {}", target, e.getMessage());
        }
    }
}