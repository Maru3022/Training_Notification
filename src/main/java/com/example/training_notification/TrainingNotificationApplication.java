package com.example.training_notification;

import jakarta.persistence.Cacheable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

//ToDo: Нагрузочные тесты
//ToDo: Юпитер тесты
//ToDo: Интеграционные тесты
//ToDo: Посмотреть как будет взаимодействовать Kafka
//ToDo: стоит ли создавать отдельный главный микросервис для взаимодействия микросервисов других, стоит их сначала написать или главный и потом от них идти

@SpringBootApplication
@Cacheable
@EnableAsync
public class TrainingNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingNotificationApplication.class, args);
    }

}
