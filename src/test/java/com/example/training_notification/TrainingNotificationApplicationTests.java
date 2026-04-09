package com.example.training_notification;

import com.example.training_notification.repository.NotificationLogRepository;
import com.example.training_notification.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    KafkaAutoConfiguration.class,
    MailSenderAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.docker.compose.enabled=false",
    "eureka.client.enabled=false",
    "telegram.bot.token=test-telegram-token",
    "telegram.enabled=true",
    "firebase.enabled=false",
    "spring.cache.type=simple"
})
class TrainingNotificationApplicationTests {

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private NotificationLogRepository notificationLogRepository;

    @Test
    void contextLoads() {
    }

}
