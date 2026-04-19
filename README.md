# Training Notification Service

A microservice notification system for a fitness platform. It consumes training events from Kafka, sends notifications over several channels (email, optional Telegram and Firebase push), caches user lookups in Redis, and can send scheduled weekly email summaries.

## Features

- **Kafka**: consumers for `training-events` and `training-topic`
- **Notifications**:
  - Email with Thymeleaf HTML templates
  - Telegram Bot API (optional, feature flag)
  - Firebase Cloud Messaging (optional, feature flag)
- **Redis**: `@Cacheable` user email lookup
- **Scheduling**: weekly email job (Sundays at 20:00 server time)
- **Async mail**: dedicated thread pool for `@Async` email sends
- **Persistence**: notification audit rows in PostgreSQL
- **REST**: test endpoint that publishes a training payload to Kafka

## Technology stack

### Backend

- **Java 17**
- **Spring Boot 3.4.13** (see `pom.xml` for the exact parent version)
- **Spring Data JPA**, **Spring Kafka**, **Spring Mail**, **Spring Web**

### Data and messaging

- **PostgreSQL 15** (typical deployment; JDBC URL is configurable)
- **Redis 7** (cache; host and port are configurable)
- **Apache Kafka** (bootstrap servers are configurable)

### Notification channels

- **JavaMail** (SMTP)
- **Thymeleaf** (HTML email)
- **Telegram Bot API** (optional)
- **Firebase Admin SDK 9.8.0** (optional; version from `pom.xml`)

### Build and quality

- **Maven** (includes Maven Wrapper under `.mvn/`)
- **Lombok**
- **Checkstyle**
- **Pitest** (optional mutation testing)
- **Testcontainers** (declared for tests; current unit tests use mocks)

### Testing

- **JUnit 5**, **Mockito**, **spring-boot-starter-test**

> This module does **not** register with Eureka or other Spring Cloud discovery clients. Any legacy Eureka keys in older configs are ignored at runtime.

## Architecture

### Components

```
┌─────────────────┐
│ REST Controller │ ── publishes to Kafka
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ Kafka topics                         │
│  - training-events                   │
│  - training-topic                    │
└────────┬────────────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌──────────┐ ┌───────────────────┐
│ Training │ │ Interaction       │
│ Listener │ │ Listener          │
└────┬─────┘ └────────┬──────────┘
     │                │
     └────────┬───────┘
              ▼
     ┌─────────────────┐
     │ Notification    │
     │ Service         │
     └────────┬────────┘
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌───────┐ ┌────────┐ ┌──────────┐
│ Email │ │Telegram│ │  Push    │
│       │ │(side   │ │(factory) │
│       │ │ path)  │ │          │
└───────┘ └────────┘ └──────────┘
```

Telegram delivery for training events is invoked from `TrainingListener` when `telegram.enabled=true` and a bot token is configured. It is **not** wired through `NotificationFactory` as a `NotificationSender`.

### Package layout

```
com.example.training_notification
├── config/           # Async, mail/caching, optional Firebase
├── controller/       # REST API
├── dto/              # Records and enums (including NotificationType)
├── entity/           # JPA entities
├── exception/        # Global exception handler
├── factory/          # Resolves NotificationSender by NotificationType
├── listener/         # Kafka listeners
├── repository/       # Spring Data repositories
└── service/
    ├── interfaces/   # NotificationSender
    ├── impl/         # Email, push, Telegram helper, user lookup
    └── scheduler/    # Notification pipeline, weekly reports
```

## Quick start

### Prerequisites

- JDK 17+
- Maven 3.9+ (or use the included wrapper: `mvnw` / `mvnw.cmd`)

### Configure

Edit `src/main/resources/application.properties` (or override with environment variables) for:

- JDBC URL and credentials (`SPRING_DATASOURCE_*`)
- Kafka (`SPRING_KAFKA_BOOTSTRAP_SERVERS`)
- Redis (`SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`)
- Mail (`SPRING_MAIL_PASSWORD`, and related SMTP fields)
- Optional Telegram (`TELEGRAM_ENABLED`, `TELEGRAM_BOT_TOKEN`)
- Optional Firebase (`FIREBASE_ENABLED` and `classpath:serviceAccountKey.json` when enabled)

### Build and run

```bash
# Windows PowerShell
.\mvnw.cmd clean verify

# Unix-like shells
./mvnw clean verify

# Run the application
.\mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run       # macOS / Linux
```

Default HTTP port: **8086** (`server.port`).

## REST API

### Publish a test training event

```http
POST /api/v1/notifications/test-send
Content-Type: application/json
```

Example body:

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

Response: **202 Accepted** (Kafka send is asynchronous).

### Typical HTTP errors

| Status | Meaning |
|--------|---------|
| 400 | Invalid input (for example, bad JSON or validation failure where applicable) |
| 500 | Unexpected server error |

## Configuration reference

### Feature toggles (defaults from `application.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `telegram.enabled` | `false` | Enables `TelegramNotificationService` and related wiring |
| `firebase.enabled` | `false` | Enables Firebase configuration and push sender when a key file is present |

### Kafka

| Topic | Consumer `groupId` in code | Role |
|-------|----------------------------|------|
| `training-events` | `notification-clean-group` | Main training pipeline in `TrainingListener` |
| `training-topic` | `notification-clean-v5` (aligned with default consumer group id property) | Secondary consumer in `InteractionListener` |

### Cron

| Job | Cron | Description |
|-----|------|-------------|
| Weekly report | `0 0 20 * * SUN` | Sends demo weekly emails (placeholder statistics until wired to real data) |

### Thread pool (`AsyncConfig`)

| Setting | Value |
|---------|-------|
| Core / max pool size | 50 / 200 |
| Queue capacity | 2000 |
| Thread name prefix | `NotificationThread-` |
| Rejection policy | `CallerRunsPolicy` |

## Testing

```bash
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd org.pitest:pitest-maven:mutationCoverage
```

## Further reading

- [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)
- [docs/METHOD_REFERENCE.md](docs/METHOD_REFERENCE.md)

## Security

Do **not** commit secrets:

- `serviceAccountKey.json` (Firebase)
- Files under `.env*` with production credentials
- Personal IDE metadata if it contains tokens

The `target/` directory is build output and should stay out of version control. The **Maven Wrapper** (`.mvn/` and `mvnw*`) is part of the project and is meant to be committed so builds are reproducible.

## Database (high level)

### `users`

| Column | Type | Notes |
|--------|------|--------|
| `id` | UUID | Primary key |
| `email` | VARCHAR | Required for lookup |

### `notification_logs`

| Column | Type | Notes |
|--------|------|--------|
| `id` | BIGINT | Surrogate key |
| `user_id` | UUID | Required in schema |
| `message` | VARCHAR(1000) | Audit text |
| `sent_at` | TIMESTAMP | When the row was written |

## Known limitations

1. Weekly report data is still **placeholder** (not aggregated from the database).
2. Publishing the **same** logical event to both Kafka topics can produce **duplicate** notifications.
3. `PushNotificationService.sendTrainingsNotification` persists a log line; **FCM** delivery uses `send(NotificationRequest)` with a topic or token string as appropriate.

## License

Proprietary.

## Contributing

1. Branch from `main`.
2. Add or extend tests for new behaviour.
3. Ensure `mvnw verify` (and Checkstyle) pass.
4. Open a pull request.

## Contact

Use your team’s usual support channel.
