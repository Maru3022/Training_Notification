package com.example.training_notification.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class TelegramNotificationService {

    @Value("${telegram.bot.token:8428567160:AAEp5qnAv_6qP5LL0ge8IqyL4W4i_p75-xs}")
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
            System.out.println("[SUCCESS] Telegram notification sent to: " + target);
        }catch (Exception e){
            System.err.println("[ERROR] Failed to send Telegram: " + e.getMessage());
            log.error("Telegram error: ", e);
        }
    }
}