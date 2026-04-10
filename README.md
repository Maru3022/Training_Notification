# Training Notification Service

A microservice notification system for a fitness platform that handles training events, sends notifications via multiple channels (Email, Telegram, Firebase Push), implements Redis caching for user data, and generates weekly scheduled reports.

## 🚀 Features

- **Kafka Integration**: Consumes events from `training-events` and `training-topic` topics
- **Multi-Channel Notifications**:
  - 📧 Email with Thymeleaf HTML templates
  - 📱 Telegram Bot API (optional)
  - 🔥 Firebase Cloud Messaging Push (optional)
- **Redis Caching**: User email lookup with `@Cacheable` annotations
- **Scheduled Reports**: Weekly email reports every Sunday at 20:00
- **Asynchronous Processing**: Thread pool executor for high-throughput notification sending
- **Database Logging**: All notifications are logged to PostgreSQL for audit trails
- **REST API**: Test endpoint for publishing training events to Kafka

## 📚 Technology Stack

### Backend
- **Java 17** - Modern Java with records and pattern matching
- **Spring Boot 3.4.6** - Latest Spring Boot framework
- **Spring Data JPA** - Database persistence layer
- **Spring Kafka** - Kafka consumer integration
- **Spring Mail** - Email sending capabilities
- **Spring Web** - REST API controllers

### Data & Messaging
- **PostgreSQL 15** - Primary relational database
- **Redis 7** - In-memory caching layer
- **Apache Kafka** - Event streaming platform (via Confluent)

### Notification Channels
- **JavaMail Sender** - SMTP email delivery
- **Thymeleaf** - HTML email template engine
- **Telegram Bot API** - Telegram messaging integration
- **Firebase Admin SDK 9.4.3** - Push notification service

### Build & Quality Tools
- **Maven** - Dependency management and build tool
- **Lombok 1.18.30** - Boilerplate reduction
- **Checkstyle 10.12.5** - Code style enforcement
- **Pitest** - Mutation testing for test quality
- **TestContainers** - Integration testing with real infrastructure

### Testing
- **JUnit 5** - Unit and integration tests
- **Mockito** - Mocking framework
- **Spring Boot Test** - Spring testing utilities

### Infrastructure
- **Docker & Docker Compose** - Containerized services (PostgreSQL, Redis, Kafka, Zookeeper)
- **Eureka Client** - Service discovery (optional)

## 🏗️ Architecture

### System Components

```
┌─────────────────┐
│ REST Controller │ ── Publishes to Kafka
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│        Kafka Topics                 │
│  - training-events                  │
│  - training-topic                   │
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
│ Sender│ │ Sender │ │ Sender   │
└───────┘ └────────┘ └──────────┘
```

### Package Structure

```
com.example.training_notification
├── config/                          # Configuration classes
│   ├── AsyncConfig.java            # Thread pool executor (50-200 threads)
│   ├── FirebaseConfig.java         # Firebase initialization
│   └── MailConfig.java             # Caching configuration
├── controller/
│   └── NotificationController.java # REST endpoints
├── dto/
│   ├── NotificationRequest.java    # Notification payload
│   ├── NotificationType.java       # EMAIL, PUSH, SMS, TELEGRAM
│   ├── TrainingDTO.java            # Training event data
│   └── UserStatsDTO.java           # Weekly report statistics
├── entity/
│   ├── NotificationLog.java        # Audit log entity
│   └── User.java                   # User entity
├── exception/
│   └── GlobalExceptionHandler.java # Centralized error handling
├── factory/
│   └── NotificationFactory.java    # Strategy pattern for senders
├── listener/
│   ├── InteractionListener.java    # Kafka: training-topic
│   └── TrainingListener.java       # Kafka: training-events
├── repository/
│   ├── NotificationLogRepository.java
│   └── UserRepository.java
└── service/
    ├── interfaces/
    │   └── NotificationSender.java # Strategy interface
    ├── impl/
    │   ├── EmailNotificationService.java
    │   ├── PushNotificationService.java
    │   ├── TelegramNotificationService.java
    │   └── UserLookupService.java  # Redis-cached user lookup
    └── scheduler/
        ├── NotificationService.java       # Core processing logic
        └── WeeklyReportScheduler.java     # Cron-based reports
```

## 🚦 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (for infrastructure services)

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL** on port `5433` (DB: `notification_db`, user: `myuser`, password: `secret`)
- **Redis** on port `6380`
- **Kafka** on port `9093`
- **Zookeeper** on port `2181`

### 2. Configure Application

Edit `src/main/resources/application.properties` to set:
- Database connection (default: `localhost:5433`)
- Kafka bootstrap servers (default: `localhost:9093`)
- Redis connection (default: `localhost:6380`)
- Email credentials (Gmail SMTP)
- Telegram bot token (if enabled)
- Firebase configuration (if enabled)

### 3. Build and Run

```bash
# Build the project
./mvnw clean install

# Run tests
./mvnw verify

# Start the application
./mvnw spring-boot:run
```

The server will start on **port 8086**.

## 📡 REST API

### Test Notification Endpoint

```http
POST /api/v1/notifications/test-send
Content-Type: application/json
```

**Request Body:**
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

**Response:** `202 Accepted` (asynchronous processing)

### Error Responses

| Status Code | Description |
|-------------|-------------|
| 400 | Bad Request - Invalid input |
| 500 | Internal Server Error - Processing failure |

## ⚙️ Configuration

### Feature Toggles

| Property | Default | Description |
|----------|---------|-------------|
| `telegram.enabled` | `true` | Enable/disable Telegram notifications |
| `firebase.enabled` | `false` | Enable/disable Firebase Push notifications |

### Kafka Topics

| Topic | Consumer Group | Purpose |
|-------|----------------|---------|
| `training-events` | `notification-clean-group` | Primary training event processing |
| `training-topic` | `notification-clean-v4` | Alternative training topic |

### Scheduled Tasks

| Task | Schedule | Description |
|------|----------|-------------|
| Weekly Report | `0 0 20 * * SUN` | Send weekly statistics every Sunday at 20:00 |

### Thread Pool Configuration

| Parameter | Value |
|-----------|-------|
| Core Pool Size | 50 |
| Maximum Pool Size | 200 |
| Queue Capacity | 2000 |
| Thread Name Prefix | `NotificationThread-` |
| Rejection Policy | `CallerRunsPolicy` |

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with code quality checks
./mvnw verify

# Run mutation testing
./mvnw org.pitest:pitest-maven:mutationCoverage
```

## 📖 Documentation

- **[API Documentation](docs/API_DOCUMENTATION.md)** - Complete method reference with all endpoints, services, and internal APIs
- **[Method Reference](docs/METHOD_REFERENCE.md)** - Detailed class-by-class method documentation

## 🔐 Security Notes

The following files are **excluded from version control**:
- `serviceAccountKey.json` - Firebase credentials
- `.env` files - Environment variables
- IDE configuration files (`.idea/`, `.vscode/`)
- Build artifacts (`target/`, `.mvn/`)

**Never commit sensitive credentials!**

## 🐳 Docker Services

All infrastructure services are defined in `compose.yaml`:

```yaml
postgres:15      → Port 5433:5432
redis:7          → Port 6380:6379
cp-zookeeper     → Port 2181
cp-kafka:7.4.0   → Port 9093:9093
```

## 📊 Database Schema

### `users` Table
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | Primary Key |
| `email` | VARCHAR | NOT NULL |

### `notification_logs` Table
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | Auto-increment PK |
| `user_id` | UUID | NOT NULL |
| `message` | VARCHAR(1000) | - |
| `sent_at` | TIMESTAMP | - |

## 🐛 Known Limitations

1. **Weekly Report Statistics**: Currently returns hardcoded demo data (not aggregated from database)
2. **Duplicate Processing**: If the same event is published to both Kafka topics, it may trigger duplicate notifications
3. **Push Notifications**: `sendTrainingsNotification()` only logs to DB; actual FCM sending requires proper device token setup

## 📝 License

This project is proprietary software.

## 👥 Contributing

1. Create a feature branch from `main`
2. Write tests for new functionality
3. Ensure `./mvnw verify` passes
4. Submit a pull request

## 📧 Contact

For questions or support, contact the development team.
