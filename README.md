# Training Notification Service

Микросервис уведомлений для фитнес-платформы: события из Kafka, email (Thymeleaf), опционально Telegram и Firebase Push, кэш email пользователей, еженедельные отчёты по расписанию.

## Документация по методам

**Полный справочник по каждому классу и методу:** [docs/METHOD_REFERENCE.md](docs/METHOD_REFERENCE.md) — REST, слушатели Kafka, сервисы, фабрика, обработчик ошибок, конфигурация, DTO и тесты.

## Возможности

- **Kafka:** топики `training-events` и `training-topic` — см. `TrainingListener`, `InteractionListener`.
- **Email:** `EmailNotificationService` + шаблон `templates/training-notification.html`.
- **Telegram:** `TelegramNotificationService` (включается свойством `telegram.enabled`).
- **Push:** `PushNotificationService` при `firebase.enabled=true` и наличии `serviceAccountKey.json`.
- **Кэш:** Redis + `@Cacheable` для email по `userId` (`UserLookupService`).
- **Расписание:** еженедельная рассылка — `WeeklyReportScheduler` (cron воскресенье 20:00).

## Быстрый старт

1. PostgreSQL, Redis, Kafka — см. `compose.yaml` и `application.properties`.
2. Переменные: пароль почты, при необходимости URL БД и Redis.
3. Запуск: `./mvnw spring-boot-run` (или `mvnw.cmd` на Windows).

## Тестовый HTTP endpoint

| Метод | Путь | Описание |
|--------|------|----------|
| POST | `/api/v1/notifications/test-send` | Публикует `TrainingDTO` в топик `training-events` (202 Accepted). |

Пример тела запроса (`TrainingDTO`):

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "telegramTag": "@user",
  "training_name": "Morning Yoga",
  "data": "2023-10-25",
  "status": "COMPLETED",
  "email": "user@example.com",
  "exercises": []
}
```

## Стек

Java 17, Spring Boot 3, Spring Kafka, Spring Data JPA, PostgreSQL, Redis, Mail, Thymeleaf, Firebase Admin (опционально).

## Сборка и качество

```bash
./mvnw verify
```

Checkstyle настроен в `pom.xml` (`checkstyle.xml`).
