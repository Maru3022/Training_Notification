# Training Notification Service

Сервис уведомлений распределённой fitness-платформы. Слушает события о тренировках и регистрации пользователей в Kafka, резолвит получателя через PostgreSQL/Redis и доставляет уведомления по email (Gmail SMTP), а также опционально через Telegram и Firebase Push. Один из шагов оркестрируемой Saga при создании пользователя — реализован через Saga-step + Transactional Outbox.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.13-brightgreen?logo=springboot)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-spring--kafka%203.3.14-black?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Hibernate%20JPA-336791?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-cache-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-distroless-2496ED?logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Kustomize-326CE5?logo=kubernetes)

## Что делает сервис

- Принимает события `training-events` и `training-topic` из Kafka и рассылает уведомления о статусе тренировки сразу по нескольким каналам (email, Telegram, Firebase Push), в зависимости от того, какие каналы сконфигурированы и есть ли у пользователя нужные контакты.
- Участвует в Saga создания пользователя как шаг `NOTIFICATION`: слушает команды `saga-notification-command` / `saga.notification.send`, отправляет приветственное письмо и публикует ответ оркестратору через Outbox, а не напрямую в Kafka.
- Гарантирует идемпотентность обработки: проверяет `correlationId` / пару `sagaId+step` в `NotificationLog` перед повторной отправкой, чтобы повторная доставка Kafka-сообщения не привела к дублю письма.
- Кэширует резолвинг email пользователя через Redis (`UserLookupService`) и поддерживает локальную копию пользователей через consumer `user.created`.
- Содержит REST-эндпоинт для ручной публикации тестового события в Kafka и шедулер еженедельных отчётов (в текущем виде использует фиксированные демоданные, а не реальную агрегацию активности пользователя — это заглушка, а не полноценная аналитика).

## Архитектура

```text
                         +---------------------+
                         |   Saga-Orchestrator  |
                         +-----------+----------+
                                     | saga-notification-command
                                     | saga.notification.send / .compensate
                                     v
+----------------+     training-events/      +----------------------------+
|  Trains-Service |--- training-topic ------->|  Training Notification    |
+----------------+                            |  Service (8086)           |
                                               |                            |
                                               |  Listener -> Notification  |
                                               |  Service -> Sender(s)      |
                                               +----+-----------+----------+
                                                    |           |
                              UserLookupService     |           | NotificationLog
                              (Redis cache) <--------+           +--> PostgreSQL
                                     |
                                     v
                          Email (SMTP) / Telegram Bot API / Firebase Cloud Messaging
                                     ^
                                     | saga-notification-response (через Outbox)
                                     |
                         +-----------+----------+
                         |   Saga-Orchestrator  |
                         +----------------------+
```

## Архитектурные решения

### 1. Saga-шаг с Transactional Outbox вместо прямой публикации ответа

`SagaNotificationCommandListener` обрабатывает команды от оркестратора (`EXECUTE` / `ROLLBACK`), отправляет письмо синхронно через `EmailNotificationService`, а ответ оркестратору (`saga-notification-response`) не публикует в Kafka напрямую внутри обработчика — он сохраняет `OutboxEvent` в той же транзакции, что и запись `NotificationLog`. Фоновый `OutboxProcessor` (`@Scheduled(fixedDelay = 3000)`) забирает события со статусом `PENDING` и публикует их в Kafka, переводя в `SENT`/`FAILED`. Это убирает классическую проблему dual-write (запись в БД прошла, а сообщение в Kafka потерялось из-за сетевого сбоя) — публикация события становится частью той же ACID-транзакции, что и бизнес-изменение.

### 2. Идемпотентность на двух уровнях саги

В репозитории одновременно есть два независимых пути обработки одной и той же предметной области:
- `NotificationSagaConsumer` — реагирует на `saga.notification.send` / `saga.notification.compensate`, идемпотентность через `existsByCorrelationId`.
- `SagaNotificationCommandListener` — реагирует на `saga-notification-command`, идемпотентность через `existsBySagaIdAndStep(sagaId, "NOTIFICATION")`.

Оба пути пишут в одну таблицу `notification_logs`, оба перед отправкой письма проверяют, не обработана ли уже эта саг-транзакция, и при повторной доставке от Kafka (at-least-once delivery) просто подтверждают оффсет без повторной отправки письма.

### 3. Dead Letter Topic и экспоненциальный backoff на уровне consumer factory

В `NotificationKafkaConfig` для слушателей `saga.notification.send` и `saga.notification.compensate` настроен `DefaultErrorHandler` с `DeadLetterPublishingRecoverer` и `ExponentialBackOff(1000ms, x2)`: сообщение, вызвавшее необработанное исключение, повторяется с растущим интервалом, а если ошибка не исчезает — уходит в DLT-топик вместо бесконечного блокирования партиции. Дополнительно продюсер настроен на `acks=all` + `enable.idempotence=true` + `max.in.flight.requests=5`, что исключает дублирование сообщений со стороны самого продюсера при ретраях.

## API-эндпоинты

| Метод | Путь | Контроллер | Описание |
|---|---|---|---|
| POST | `/api/v1/notifications/test-send` | `NotificationController` | Публикует `TrainingDTO` в топик `training-events` для ручного тестирования цепочки уведомлений |

> Сервис в основном управляется событиями Kafka, а не REST — собственный публичный API минимален.

### Ключевые Kafka-топики

| Топик | Направление | Слушатель/источник |
|---|---|---|
| `training-events`, `training-topic` | consume | `TrainingListener`, `InteractionListener` |
| `user.created` | consume | `UserSyncListener` |
| `saga.notification.send` | consume | `NotificationSagaConsumer` |
| `saga.notification.compensate` | consume | `NotificationSagaConsumer` |
| `saga-notification-command` | consume | `SagaNotificationCommandListener` |
| `saga-notification-response` | produce (через Outbox) | `SagaNotificationCommandListener` → `OutboxProcessor` |

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык / платформа | Java 17, Spring Boot 3.4.13 |
| Данные | PostgreSQL (Spring Data JPA, Hikari), Redis (Spring Cache) |
| Messaging | Apache Kafka 3.9.2, Spring Kafka 3.3.14, DLT + ExponentialBackOff |
| Каналы доставки | Spring Mail (Gmail SMTP), Telegram Bot API (опционально), Firebase Admin SDK (опционально) |
| Шаблонизация | Thymeleaf |
| Тестирование | JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka) |
| Качество кода | Checkstyle, JaCoCo, Pitest (мутационное тестирование) |
| CI/CD | GitHub Actions: Checkstyle, Hadolint, тесты с Postgres/Redis-сервисами, Trivy (FS + image), SBOM/provenance, kubeconform, деплой в Kubernetes |
| Контейнеризация | Docker (distroless `java17-debian12:nonroot`) |
| Деплой | Kubernetes + Kustomize (`k8s/base`, `k8s/overlays/prod`), HPA, PDB, NetworkPolicy, readiness/liveness probes |

## Локальный запуск

### Зависимости

JDK 17+, Maven Wrapper, PostgreSQL, Redis, Kafka.

### Переменные окружения

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/notification_db
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=secret
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6380
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=
FIREBASE_ENABLED=false
MANAGEMENT_SERVER_PORT=8081
```

### Сборка и тесты

```bash
./mvnw clean verify
```

### Запуск

```bash
./mvnw spring-boot:run
```

Сервис поднимется на `localhost:8086`, актуатор — на `localhost:8081` (`/actuator/health`, `/actuator/prometheus`).

## Связанные репозитории

- [Saga-Orchestrator](https://github.com/Maru3022/Saga-Orchestrator) — оркестратор саги создания пользователя, источник команд для этого сервиса
- [Trains-Service](https://github.com/Maru3022/Trains-Service) — публикует события о тренировках в `training-events`/`training-topic`
- [Training-Nutrition](https://github.com/Maru3022/Training-Nutrition) — сервис расчёта питания, соседний шаг той же платформы
- [Eureka-server](https://github.com/Maru3022/Eureka-server) — service discovery для всей платформы
